package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Turns a search expression into a {@link Criterion}.
 *
 * <pre>
 * expr  := and ( "||" and )*
 * and   := unary ( "&&" unary )*
 * unary := "!" unary | "(" expr ")" | atom
 * atom  := "type" "=" TYPE
 *        | TEXT_FIELD "contains" STRING
 *        | TIME_FIELD ( "&lt;" | "&gt;" | "=" ) DATETIME
 * </pre>
 *
 * "!" binds tightest, then "&&", then "||". Strings and date-times are written
 * in double quotes; inside a string, {@code \"} and {@code \\} are escapes.
 * Date-times use {@code yyyy-MM-dd HH:mm} or {@code yyyy-MM-dd HH:mm:ss}.
 */
public class CriterionParser {
    private static final DateTimeFormatter WITH_SECONDS =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter WITHOUT_SECONDS =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private static final Set<String> TYPES = Set.of("note", "task", "event", "contact");
    private static final Set<String> TEXT_FIELDS =
            Set.of("text", "description", "name", "address", "mobile");
    private static final Set<String> TIME_FIELDS = Set.of("deadline", "start", "alarm");

    private enum Kind { LPAREN, RPAREN, AND, OR, NOT, LT, GT, EQ, STRING, WORD, END }

    private static final class Token {
        final Kind kind;
        final String text;
        final int position;

        Token(Kind kind, String text, int position) {
            this.kind = kind;
            this.text = text;
            this.position = position;
        }
    }

    private final List<Token> tokens;
    private int pos = 0;

    private CriterionParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Parses a search expression.
     *
     * @throws InvalidCriterionException if the expression is empty or malformed
     */
    public static Criterion parse(String expression) throws InvalidCriterionException {
        if (expression == null || expression.trim().isEmpty()) {
            throw new InvalidCriterionException("Search expression is empty.");
        }
        CriterionParser parser = new CriterionParser(tokenize(expression));
        Criterion result = parser.parseOr();
        Token extra = parser.peek();
        if (extra.kind != Kind.END) {
            throw parser.error("Unexpected '" + extra.text + "'", extra);
        }
        return result;
    }

    private static List<Token> tokenize(String s) throws InvalidCriterionException {
        List<Token> out = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (c == '(') {
                out.add(new Token(Kind.LPAREN, "(", i++));
            } else if (c == ')') {
                out.add(new Token(Kind.RPAREN, ")", i++));
            } else if (c == '!') {
                out.add(new Token(Kind.NOT, "!", i++));
            } else if (c == '<') {
                out.add(new Token(Kind.LT, "<", i++));
            } else if (c == '>') {
                out.add(new Token(Kind.GT, ">", i++));
            } else if (c == '=') {
                out.add(new Token(Kind.EQ, "=", i++));
            } else if (c == '&' || c == '|') {
                if (i + 1 >= s.length() || s.charAt(i + 1) != c) {
                    throw new InvalidCriterionException(
                            "Expected '" + c + c + "' at position " + i + " but found a single '" + c + "'.");
                }
                out.add(new Token(c == '&' ? Kind.AND : Kind.OR, "" + c + c, i));
                i += 2;
            } else if (c == '"') {
                int start = i++;
                StringBuilder sb = new StringBuilder();
                boolean closed = false;
                while (i < s.length()) {
                    char d = s.charAt(i++);
                    if (d == '"') {
                        closed = true;
                        break;
                    }
                    if (d == '\\') {
                        if (i >= s.length()) {
                            break;
                        }
                        d = s.charAt(i++);
                    }
                    sb.append(d);
                }
                if (!closed) {
                    throw new InvalidCriterionException(
                            "Unterminated string starting at position " + start + ".");
                }
                out.add(new Token(Kind.STRING, sb.toString(), start));
            } else if (Character.isLetterOrDigit(c) || c == '_') {
                int start = i;
                while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_')) {
                    i++;
                }
                out.add(new Token(Kind.WORD, s.substring(start, i), start));
            } else {
                throw new InvalidCriterionException(
                        "Unexpected character '" + c + "' at position " + i + ".");
            }
        }
        out.add(new Token(Kind.END, "end of expression", s.length()));
        return out;
    }

    private Criterion parseOr() throws InvalidCriterionException {
        Criterion left = parseAnd();
        while (peek().kind == Kind.OR) {
            next();
            left = new OrCriterion(left, parseAnd());
        }
        return left;
    }

    private Criterion parseAnd() throws InvalidCriterionException {
        Criterion left = parseUnary();
        while (peek().kind == Kind.AND) {
            next();
            left = new AndCriterion(left, parseUnary());
        }
        return left;
    }

    private Criterion parseUnary() throws InvalidCriterionException {
        Token t = peek();
        if (t.kind == Kind.NOT) {
            next();
            return new NotCriterion(parseUnary());
        }
        if (t.kind == Kind.LPAREN) {
            next();
            Criterion inner = parseOr();
            expect(Kind.RPAREN, "')'");
            return inner;
        }
        return parseAtom();
    }

    private Criterion parseAtom() throws InvalidCriterionException {
        Token field = next();
        if (field.kind != Kind.WORD) {
            throw error("Expected a field name but found '" + field.text + "'", field);
        }
        String name = field.text;
        if (name.equals("type")) {
            expect(Kind.EQ, "'='");
            Token value = next();
            if ((value.kind != Kind.WORD && value.kind != Kind.STRING) || !TYPES.contains(value.text)) {
                throw error("Unknown type '" + value.text + "' (use note, task, event or contact)", value);
            }
            return new TypeCriterion(value.text);
        }
        if (TEXT_FIELDS.contains(name)) {
            Token op = next();
            if (op.kind != Kind.WORD || !op.text.equals("contains")) {
                throw error("Expected 'contains' after '" + name + "' but found '" + op.text + "'", op);
            }
            Token value = next();
            if (value.kind != Kind.STRING) {
                throw error("Expected a quoted string after 'contains' but found '" + value.text + "'", value);
            }
            return new TextContainsCriterion(name, value.text);
        }
        if (TIME_FIELDS.contains(name)) {
            Token op = next();
            TimeCompareCriterion.Comparison comparison;
            if (op.kind == Kind.LT) {
                comparison = TimeCompareCriterion.Comparison.BEFORE;
            } else if (op.kind == Kind.GT) {
                comparison = TimeCompareCriterion.Comparison.AFTER;
            } else if (op.kind == Kind.EQ) {
                comparison = TimeCompareCriterion.Comparison.EQUAL;
            } else {
                throw error("Expected '<', '>' or '=' after '" + name + "' but found '" + op.text + "'", op);
            }
            Token value = next();
            if (value.kind != Kind.STRING) {
                throw error("Expected a quoted date-time like \"2026-11-20 20:00\" but found '"
                        + value.text + "'", value);
            }
            return new TimeCompareCriterion(name, comparison, parseDateTime(value));
        }
        throw error("Unknown field '" + name + "'", field);
    }

    private LocalDateTime parseDateTime(Token value) throws InvalidCriterionException {
        try {
            return LocalDateTime.parse(value.text, WITH_SECONDS);
        } catch (DateTimeParseException ignored) {
            // fall through to the format without seconds
        }
        try {
            return LocalDateTime.parse(value.text, WITHOUT_SECONDS);
        } catch (DateTimeParseException e) {
            throw error("Invalid date-time \"" + value.text
                    + "\" (use yyyy-MM-dd HH:mm or yyyy-MM-dd HH:mm:ss)", value);
        }
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private Token next() {
        Token t = tokens.get(pos);
        if (t.kind != Kind.END) {
            pos++;
        }
        return t;
    }

    private void expect(Kind kind, String description) throws InvalidCriterionException {
        Token t = next();
        if (t.kind != kind) {
            throw error("Expected " + description + " but found '" + t.text + "'", t);
        }
    }

    private InvalidCriterionException error(String message, Token at) {
        return new InvalidCriterionException(message + " at position " + at.position + ".");
    }
}

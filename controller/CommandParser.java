package controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns an input line into a {@link Command}.
 * Words are separated by spaces. A value in double quotes may contain spaces;
 * inside quotes, \" and \\ are escapes.
 */
public class CommandParser {

    private CommandParser() {
    }

    public static Command parse(String line) throws InvalidCommandException {
        if (line == null || line.trim().isEmpty()) {
            throw new InvalidCommandException("Empty command. Type 'help' for a list of commands.");
        }
        String trimmed = line.trim();
        int space = trimmed.indexOf(' ');
        String name = (space < 0 ? trimmed : trimmed.substring(0, space)).toLowerCase();
        String rest = space < 0 ? "" : trimmed.substring(space + 1).trim();

        List<String> words = new ArrayList<>();
        Map<String, String> fields = new LinkedHashMap<>();
        if (name.equals("save") || name.equals("load")) {
            // A path can contain '=' without being a field assignment.
            words.addAll(split(rest, true));
        } else if (!name.equals("search")) { // search text is handled by CriterionParser
            for (String token : split(rest, false)) {
                int eq = token.indexOf('=');
                if (eq < 0) {
                    words.add(token);
                } else if (eq == 0) {
                    throw new InvalidCommandException("Missing field name before '=' in: " + token);
                } else {
                    String field = token.substring(0, eq);
                    if (fields.containsKey(field)) {
                        throw new InvalidCommandException("Field '" + field + "' is given twice.");
                    }
                    fields.put(field, token.substring(eq + 1));
                }
            }
        }
        return new Command(name, words, fields, rest);
    }

    /** Splits on spaces, keeping quoted parts together and removing the quotes. */
    private static List<String> split(String s, boolean literalPath) throws InvalidCommandException {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean hasToken = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    inQuotes = false;
                } else if (c == '\\' && i + 1 < s.length()
                        && (!literalPath || s.charAt(i + 1) == '"' || s.charAt(i + 1) == '\\')) {
                    current.append(s.charAt(++i));
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
                hasToken = true;
            } else if (Character.isWhitespace(c)) {
                if (hasToken) {
                    out.add(current.toString());
                    current.setLength(0);
                    hasToken = false;
                }
            } else {
                current.append(c);
                hasToken = true;
            }
        }
        if (inQuotes) {
            throw new InvalidCommandException("Missing closing quote.");
        }
        if (hasToken) {
            out.add(current.toString());
        }
        return out;
    }
}

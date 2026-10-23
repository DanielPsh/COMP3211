package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for CriterionParser: grammar, precedence, matching and error handling,
 * run against Part A's real PIR classes.
 */
class CriterionParserTest {
    private static String time(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute).format(PIR.TIME_FORMAT);
    }

    private final Note note = new Note(1, "Buy milk");
    private final Task draftTask = new Task(2, "draft report", time(2026, 11, 10, 12, 0));
    private final Task finalTask = new Task(3, "Submit SE project", time(2026, 11, 20, 20, 0));
    private final Contact contact = new Contact(4, "Chan Tai Man", "Hung Hom", "91234567");
    private final Event meeting = new Event(5, "Group meeting",
            time(2026, 10, 24, 14, 0), time(2026, 10, 24, 13, 45));

    private Criterion parse(String expr) throws InvalidCriterionException {
        return CriterionParser.parse(expr);
    }

    // Checks: "type = task" accepts tasks and rejects other types.
    @Test
    void parse_typeAtom() throws Exception {
        Criterion c = parse("type = task");
        assertTrue(c.matches(finalTask));
        assertFalse(c.matches(note));
    }

    // Checks: "contains" works on text fields, e.g. mobile.
    @Test
    void parse_containsAtom() throws Exception {
        assertTrue(parse("mobile contains \"1234\"").matches(contact));
        assertFalse(parse("mobile contains \"999\"").matches(contact));
    }

    // Checks: < > = on a time field; deadline < 2026-11-15 is true for the earlier task only.
    @Test
    void parse_timeAtoms() throws Exception {
        Criterion before = parse("deadline < \"2026-11-15 00:00\"");
        assertTrue(before.matches(draftTask));
        assertFalse(before.matches(finalTask));

        assertTrue(parse("deadline > \"2026-11-15 00:00\"").matches(finalTask));
        assertTrue(parse("deadline = \"2026-11-20 20:00\"").matches(finalTask));
    }

    // Checks: both event time fields (start and alarm) can be searched.
    @Test
    void parse_eventStartAndAlarm() throws Exception {
        assertTrue(parse("start = \"2026-10-24 14:00\"").matches(meeting));
        assertTrue(parse("alarm < \"2026-10-24 14:00\"").matches(meeting));
        assertFalse(parse("alarm > \"2026-10-24 14:00\"").matches(meeting));
    }

    // Checks: date-times are accepted with and without seconds.
    @Test
    void parse_dateTime_withAndWithoutSeconds() throws Exception {
        assertTrue(parse("deadline = \"2026-11-20 20:00:00\"").matches(finalTask));
        assertTrue(parse("deadline = \"2026-11-20 20:00\"").matches(finalTask));
        assertFalse(parse("deadline = \"2026-11-20 20:00:01\"").matches(finalTask));
    }

    // Checks: the combined example from the team plan.
    @Test
    void parse_exampleFromPlan() throws Exception {
        Criterion c = parse("type = task && deadline < \"2026-11-30 00:00\" && !(description contains \"draft\")");
        assertTrue(c.matches(finalTask));
        assertFalse(c.matches(draftTask));
        assertFalse(c.matches(note));
    }

    // Checks: "a || b && c" is read as "a || (b && c)".
    @Test
    void precedence_andBindsTighterThanOr() throws Exception {
        Criterion c = parse("type = note || type = task && description contains \"draft\"");
        assertEquals("(type = note || (type = task && description contains \"draft\"))", c.toString());
        assertTrue(c.matches(note));
        assertTrue(c.matches(draftTask));
        assertFalse(c.matches(finalTask));
    }

    // Checks: "!a && b" is read as "(!a) && b".
    @Test
    void precedence_notBindsTighterThanAnd() throws Exception {
        Criterion c = parse("!type = note && type = task");
        assertEquals("(!type = note && type = task)", c.toString());
    }

    // Checks: brackets override the default precedence.
    @Test
    void precedence_bracketsOverrideDefault() throws Exception {
        Criterion c = parse("(type = note || type = task) && description contains \"draft\"");
        assertEquals("((type = note || type = task) && description contains \"draft\")", c.toString());
        assertFalse(c.matches(note));
        assertTrue(c.matches(draftTask));
    }

    // Checks: chained && is left-associative; double negation cancels out.
    @Test
    void operators_areLeftAssociativeAndChainable() throws Exception {
        assertEquals("((type = task && description contains \"a\") && description contains \"b\")",
                parse("type = task && description contains \"a\" && description contains \"b\"").toString());
        assertTrue(parse("!!type = task").matches(finalTask));
    }

    // Checks: \" is a quote and \\ is a backslash inside a string.
    @Test
    void string_escapesQuoteAndBackslash() throws Exception {
        Note quoted = new Note(6, "say \"hi\" \\ bye");
        assertTrue(parse("text contains \"say \\\"hi\\\" \\\\ bye\"").matches(quoted));
    }

    // Checks: no spaces around operators and brackets is fine.
    @Test
    void whitespace_isFlexible() throws Exception {
        Criterion c = parse("(type=task)&&!(description contains \"draft\")");
        assertTrue(c.matches(finalTask));
        assertFalse(c.matches(draftTask));
    }

    // Checks: empty, blank and null expressions are rejected.
    @Test
    void invalid_emptyOrNull_throws() {
        assertThrows(InvalidCriterionException.class, () -> parse(""));
        assertThrows(InvalidCriterionException.class, () -> parse("   "));
        assertThrows(InvalidCriterionException.class, () -> parse(null));
    }

    // Checks: unknown types and unknown field names are rejected.
    @Test
    void invalid_unknownTypeOrField_throws() {
        assertThrows(InvalidCriterionException.class, () -> parse("type = banana"));
        assertThrows(InvalidCriterionException.class, () -> parse("color contains \"red\""));
    }

    // Checks: "contains" only works on text fields, < > = only on time fields.
    @Test
    void invalid_wrongOperatorForField_throws() {
        assertThrows(InvalidCriterionException.class, () -> parse("deadline contains \"2026\""));
        assertThrows(InvalidCriterionException.class, () -> parse("description < \"2026-11-20 20:00\""));
        assertThrows(InvalidCriterionException.class, () -> parse("type < task"));
    }

    // Checks: malformed or impossible date-times are rejected.
    @Test
    void invalid_badDateTime_throws() {
        assertThrows(InvalidCriterionException.class, () -> parse("deadline < \"2026-11-20\""));
        assertThrows(InvalidCriterionException.class, () -> parse("deadline < \"2026-02-30 10:00\""));
        assertThrows(InvalidCriterionException.class, () -> parse("deadline < \"2026-13-01 10:00\""));
        assertThrows(InvalidCriterionException.class, () -> parse("deadline < 2026"));
    }

    // Checks: misplaced operators, brackets, quotes and stray characters are rejected.
    @Test
    void invalid_syntaxErrors_throw() {
        assertThrows(InvalidCriterionException.class, () -> parse("type = task &&"));
        assertThrows(InvalidCriterionException.class, () -> parse("&& type = task"));
        assertThrows(InvalidCriterionException.class, () -> parse("type = task & type = note"));
        assertThrows(InvalidCriterionException.class, () -> parse("(type = task"));
        assertThrows(InvalidCriterionException.class, () -> parse("type = task)"));
        assertThrows(InvalidCriterionException.class, () -> parse("type = task type = note"));
        assertThrows(InvalidCriterionException.class, () -> parse("text contains \"unterminated"));
        assertThrows(InvalidCriterionException.class, () -> parse("text contains milk"));
        assertThrows(InvalidCriterionException.class, () -> parse("type = task @"));
    }
}

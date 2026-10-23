package model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the individual Criterion classes (no parsing involved),
 * run against Part A's real PIR classes.
 */
class CriteriaTest {
    private static String time(int year, int month, int day, int hour, int minute) {
        return LocalDateTime.of(year, month, day, hour, minute).format(PIR.TIME_FORMAT);
    }

    private final Note note = new Note(1, "Buy milk");
    private final Task task = new Task(2, "Submit SE project", time(2026, 11, 20, 20, 0));

    // Checks: a "task" criterion accepts a task and rejects a note.
    @Test
    void typeCriterion_matchesOnlyItsOwnType() {
        Criterion c = new TypeCriterion("task");
        assertTrue(c.matches(task));
        assertFalse(c.matches(note));
    }

    // Checks: "milk" is inside "Buy milk", but "Milk" (different case) is not.
    @Test
    void textContains_matchesSubstring_caseSensitive() {
        assertTrue(new TextContainsCriterion("text", "milk").matches(note));
        assertFalse(new TextContainsCriterion("text", "Milk").matches(note));
    }

    // Checks: a note has no "description" field, so the criterion cannot match.
    @Test
    void textContains_fieldMissing_isFalse() {
        assertFalse(new TextContainsCriterion("description", "").matches(note));
    }

    // Checks: before / after / equal against a task deadline of 2026-11-20 20:00.
    @Test
    void timeCompare_beforeAfterEqual() {
        LocalDateTime later = LocalDateTime.of(2026, 11, 21, 0, 0);
        LocalDateTime same = LocalDateTime.of(2026, 11, 20, 20, 0);
        LocalDateTime earlier = LocalDateTime.of(2026, 11, 1, 0, 0);

        assertTrue(new TimeCompareCriterion("deadline", TimeCompareCriterion.Comparison.BEFORE, later).matches(task));
        assertFalse(new TimeCompareCriterion("deadline", TimeCompareCriterion.Comparison.BEFORE, same).matches(task));
        assertTrue(new TimeCompareCriterion("deadline", TimeCompareCriterion.Comparison.AFTER, earlier).matches(task));
        assertFalse(new TimeCompareCriterion("deadline", TimeCompareCriterion.Comparison.AFTER, same).matches(task));
        assertTrue(new TimeCompareCriterion("deadline", TimeCompareCriterion.Comparison.EQUAL, same).matches(task));
        assertFalse(new TimeCompareCriterion("deadline", TimeCompareCriterion.Comparison.EQUAL, earlier).matches(task));
    }

    // Checks: a note has no deadline, so every comparison is false.
    @Test
    void timeCompare_fieldMissing_isFalse() {
        LocalDateTime any = LocalDateTime.of(2026, 1, 1, 0, 0);
        assertFalse(new TimeCompareCriterion("deadline", TimeCompareCriterion.Comparison.BEFORE, any).matches(note));
        assertFalse(new TimeCompareCriterion("deadline", TimeCompareCriterion.Comparison.AFTER, any).matches(note));
        assertFalse(new TimeCompareCriterion("deadline", TimeCompareCriterion.Comparison.EQUAL, any).matches(note));
    }

    // Checks: And / Or / Not behave as logical operators.
    @Test
    void andOrNot_combineAsLogicalOperators() {
        Criterion isTask = new TypeCriterion("task");
        Criterion isNote = new TypeCriterion("note");

        assertFalse(new AndCriterion(isTask, isNote).matches(task));
        assertTrue(new OrCriterion(isTask, isNote).matches(task));
        assertTrue(new OrCriterion(isTask, isNote).matches(note));
        assertTrue(new NotCriterion(isNote).matches(task));
        assertFalse(new NotCriterion(isNote).matches(note));
    }

    // Checks: a missing field makes the inner criterion false, so "!" makes it true.
    @Test
    void not_ofCriterionOnMissingField_isTrue() {
        Criterion c = new NotCriterion(new TextContainsCriterion("description", "x"));
        assertTrue(c.matches(note));
    }
}

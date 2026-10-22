package model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Task (US3: tasks with a description and a deadline).
 * Dates are built with PIR.TIME_FORMAT so the tests do not depend on the chosen format.
 */
class TaskTest {

    private static final String DEADLINE = LocalDateTime.of(2026, 11, 20, 20, 0).format(PIR.TIME_FORMAT);
    private static final String NEW_DEADLINE = LocalDateTime.of(2026, 11, 19, 23, 59).format(PIR.TIME_FORMAT);

    private Task newTask() {
        return new Task(3, "Submit SE project", DEADLINE);
    }

    // Checks: a task reports its type as "task".
    @Test
    void getType_isTask() {
        assertEquals("task", newTask().getType());
    }

    // Checks: the constructor stores the ID, the description and the deadline.
    @Test
    void constructor_storesAllFields() {
        Task task = newTask();

        assertEquals(3, task.getId());
        assertEquals(Map.of("description", "Submit SE project"), task.get_text_fields());
        assertEquals(Map.of("deadline", DEADLINE), task.get_time_fields());
    }

    // Checks: the declared field names are "description" (text) and "deadline" (time).
    @Test
    void fieldNames_matchSpecification() {
        Task task = newTask();

        assertEquals(Set.of("description"), task.getTextFields());
        assertEquals(Set.of("deadline"), task.getTimeFields());
    }

    // Checks (US6): set_field() on "description" replaces the description.
    @Test
    void setField_description_updatesDescription() {
        Task task = newTask();

        task.set_field("description", "Submit final report");

        assertEquals("Submit final report", task.get_text_fields().get("description"));
    }

    // Checks (US6): set_field() on "deadline" replaces the deadline.
    @Test
    void setField_deadline_updatesDeadline() {
        Task task = newTask();

        task.set_field("deadline", NEW_DEADLINE);

        assertEquals(NEW_DEADLINE, task.get_time_fields().get("deadline"));
    }

    // Checks: set_field() rejects a field that tasks do not have.
    @Test
    void setField_fieldOfOtherType_throws() {
        assertThrows(InvalidFieldException.class, () -> newTask().set_field("start", NEW_DEADLINE));
    }

    // Checks: set_field() rejects a deadline that is not a valid date-time with
    // InvalidFieldException (plan 4.1), and the old deadline is kept.
    @Test
    void setField_invalidDeadline_throwsInvalidFieldException() {
        Task task = newTask();

        assertThrows(InvalidFieldException.class, () -> task.set_field("deadline", "next Friday"));

        assertEquals(DEADLINE, task.get_time_fields().get("deadline"));
    }

    // Checks: the constructor rejects a deadline that is not a valid date-time
    // with InvalidFieldException (plan 4.1).
    @Test
    void constructor_invalidDeadline_throwsInvalidFieldException() {
        assertThrows(InvalidFieldException.class, () -> new Task(3, "Submit SE project", "next Friday"));
    }

    // Checks (US8): the detail string shows the ID, description and deadline.
    @Test
    void toDetailString_containsAllFields() {
        String details = newTask().to_detail_string();

        assertTrue(details.contains("3"));
        assertTrue(details.contains("Submit SE project"));
        assertTrue(details.contains(DEADLINE));
    }

    // Checks (US10): to_record() gives the ID, type and every field, ready to be saved.
    @Test
    void toRecord_containsIdTypeAndFields() {
        Map<String, String> expected = Map.of(
                "id", "3", "type", "task", "description", "Submit SE project", "deadline", DEADLINE);

        assertEquals(expected, newTask().to_record());
    }
}

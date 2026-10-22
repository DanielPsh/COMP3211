package model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the behaviour shared by every record, defined in the abstract PIR class.
 * Task is used as the concrete record because it has both a text field and a time field.
 */
class PIRTest {

    private static final String DEADLINE = LocalDateTime.of(2026, 11, 20, 20, 0).format(PIR.TIME_FORMAT);

    private Task newTask() {
        return new Task(7, "Submit SE project", DEADLINE);
    }

    // Checks: getId() returns the ID passed to the constructor.
    @Test
    void getId_returnsIdGivenToConstructor() {
        assertEquals(7, newTask().getId());
    }

    // Checks: a date-time formatted with TIME_FORMAT parses back to the same value,
    // so dates survive being printed and saved.
    @Test
    void timeFormat_formatThenParse_givesSameValue() {
        LocalDateTime original = LocalDateTime.of(2026, 11, 20, 20, 0);

        LocalDateTime parsed = LocalDateTime.parse(original.format(PIR.TIME_FORMAT), PIR.TIME_FORMAT);

        assertEquals(original, parsed);
    }

    // Checks: validateFieldName() accepts a text field that the record type has.
    @Test
    void validateFieldName_textFieldOfType_isAccepted() {
        assertDoesNotThrow(() -> newTask().validateFieldName("description"));
    }

    // Checks: validateFieldName() accepts a time field that the record type has.
    @Test
    void validateFieldName_timeFieldOfType_isAccepted() {
        assertDoesNotThrow(() -> newTask().validateFieldName("deadline"));
    }

    // Checks: validateFieldName() rejects a field the record type does not have,
    // and the error message names the field and the record type.
    @Test
    void validateFieldName_fieldOfOtherType_throwsWithHelpfulMessage() {
        InvalidFieldException e = assertThrows(InvalidFieldException.class,
                () -> newTask().validateFieldName("mobile"));

        assertTrue(e.getMessage().contains("mobile"));
        assertTrue(e.getMessage().contains("task"));
    }

    // Checks: set_field() with a valid field name stores the new value.
    @Test
    void setField_validName_updatesValue() {
        Task task = newTask();

        task.set_field("description", "Submit final report");

        assertEquals("Submit final report", task.get_text_fields().get("description"));
    }

    // Checks: set_field() validates the name first, so an unknown field is rejected
    // with InvalidFieldException and the record is left unchanged.
    @Test
    void setField_unknownName_throwsAndLeavesRecordUnchanged() {
        Task task = newTask();

        assertThrows(InvalidFieldException.class, () -> task.set_field("dedline", DEADLINE));

        assertEquals("Submit SE project", task.get_text_fields().get("description"));
        assertEquals(DEADLINE, task.get_time_fields().get("deadline"));
    }
}

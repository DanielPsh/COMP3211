package model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for PIRFactory (US1: one place that creates any type of record,
 * used by the create command and by loading a .pim file).
 */
class PIRFactoryTest {

    private static final String TIME_1 = LocalDateTime.of(2026, 11, 20, 20, 0).format(PIR.TIME_FORMAT);
    private static final String TIME_2 = LocalDateTime.of(2026, 11, 20, 19, 45).format(PIR.TIME_FORMAT);

    // Checks: the supported types are exactly the four record types.
    @Test
    void supportedTypes_areTheFourRecordTypes() {
        assertEquals(Set.of("note", "task", "event", "contact"), PIRFactory.SUPPORTED_TYPES);
    }

    // Checks (US2): "note" creates a Note with the given ID and text.
    @Test
    void create_note_returnsNoteWithFields() {
        PIR pir = PIRFactory.create(1, "note", Map.of("text", "Buy milk"));

        assertInstanceOf(Note.class, pir);
        assertEquals(1, pir.getId());
        assertEquals(Map.of("text", "Buy milk"), pir.get_text_fields());
    }

    // Checks (US3): "task" creates a Task with the given ID, description and deadline.
    @Test
    void create_task_returnsTaskWithFields() {
        PIR pir = PIRFactory.create(2, "task", Map.of("description", "Submit SE project", "deadline", TIME_1));

        assertInstanceOf(Task.class, pir);
        assertEquals(2, pir.getId());
        assertEquals(Map.of("description", "Submit SE project"), pir.get_text_fields());
        assertEquals(Map.of("deadline", TIME_1), pir.get_time_fields());
    }

    // Checks (US4): "event" creates an Event with the given ID, description, start and alarm.
    @Test
    void create_event_returnsEventWithFields() {
        PIR pir = PIRFactory.create(3, "event",
                Map.of("description", "Group meeting", "start", TIME_1, "alarm", TIME_2));

        assertInstanceOf(Event.class, pir);
        assertEquals(3, pir.getId());
        assertEquals(Map.of("description", "Group meeting"), pir.get_text_fields());
        assertEquals(Map.of("start", TIME_1, "alarm", TIME_2), pir.get_time_fields());
    }

    // Checks (US5): "contact" creates a Contact with the given ID, name, address and mobile.
    @Test
    void create_contact_returnsContactWithFields() {
        Map<String, String> fields = Map.of("name", "Chan Tai Man", "address", "Hung Hom", "mobile", "91234567");

        PIR pir = PIRFactory.create(4, "contact", fields);

        assertInstanceOf(Contact.class, pir);
        assertEquals(4, pir.getId());
        assertEquals(fields, pir.get_text_fields());
    }

    // Checks: the type name is case-insensitive.
    @Test
    void create_upperCaseType_isAccepted() {
        assertInstanceOf(Note.class, PIRFactory.create(1, "NOTE", Map.of("text", "Buy milk")));
    }

    // Checks: an unknown type is rejected, and the message names the bad type.
    @Test
    void create_unknownType_throwsWithTypeInMessage() {
        InvalidFieldException e = assertThrows(InvalidFieldException.class,
                () -> PIRFactory.create(1, "meeting", Map.of("text", "x")));

        assertTrue(e.getMessage().contains("meeting"));
    }

    // Checks: a missing (null) type is rejected.
    @Test
    void create_nullType_throws() {
        assertThrows(InvalidFieldException.class, () -> PIRFactory.create(1, null, Map.of("text", "x")));
    }

    // Checks: a field that does not belong to the type is rejected,
    // e.g. "create note text=... deadline=...".
    @Test
    void create_extraFieldNotOfType_throws() {
        assertThrows(InvalidFieldException.class,
                () -> PIRFactory.create(1, "note", Map.of("text", "Buy milk", "deadline", TIME_1)));
    }

    // Checks: an invalid date-time value is reported as InvalidFieldException (plan 4.1),
    // so the controller only has to handle one exception type.
    @Test
    void create_invalidTimeValue_throwsInvalidFieldException() {
        assertThrows(InvalidFieldException.class,
                () -> PIRFactory.create(2, "task", Map.of("description", "Submit", "deadline", "soon")));
    }
}

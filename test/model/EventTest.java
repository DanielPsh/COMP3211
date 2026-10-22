package model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Event (US4: events with a description, a starting time and an alarm).
 * Dates are built with PIR.TIME_FORMAT so the tests do not depend on the chosen format.
 */
class EventTest {

    private static final String START = LocalDateTime.of(2026, 10, 24, 14, 0).format(PIR.TIME_FORMAT);
    private static final String ALARM = LocalDateTime.of(2026, 10, 24, 13, 45).format(PIR.TIME_FORMAT);
    private static final String LATER = LocalDateTime.of(2026, 10, 25, 9, 30).format(PIR.TIME_FORMAT);

    private Event newEvent() {
        return new Event(4, "Group meeting", START, ALARM);
    }

    // Checks: an event reports its type as "event".
    @Test
    void getType_isEvent() {
        assertEquals("event", newEvent().getType());
    }

    // Checks: the constructor stores the ID, the description, the start and the alarm.
    @Test
    void constructor_storesAllFields() {
        Event event = newEvent();

        assertEquals(4, event.getId());
        assertEquals(Map.of("description", "Group meeting"), event.get_text_fields());
        assertEquals(Map.of("start", START, "alarm", ALARM), event.get_time_fields());
    }

    // Checks: the declared field names are "description" (text) and "start", "alarm" (time).
    @Test
    void fieldNames_matchSpecification() {
        Event event = newEvent();

        assertEquals(Set.of("description"), event.getTextFields());
        assertEquals(Set.of("start", "alarm"), event.getTimeFields());
    }

    // Checks (US6): set_field() on "description" replaces the description.
    @Test
    void setField_description_updatesDescription() {
        Event event = newEvent();

        event.set_field("description", "Project demo");

        assertEquals("Project demo", event.get_text_fields().get("description"));
    }

    // Checks (US6): set_field() on "start" changes only the start time.
    @Test
    void setField_start_updatesStartOnly() {
        Event event = newEvent();

        event.set_field("start", LATER);

        assertEquals(LATER, event.get_time_fields().get("start"));
        assertEquals(ALARM, event.get_time_fields().get("alarm"));
    }

    // Checks (US6): set_field() on "alarm" changes only the alarm time.
    @Test
    void setField_alarm_updatesAlarmOnly() {
        Event event = newEvent();

        event.set_field("alarm", LATER);

        assertEquals(LATER, event.get_time_fields().get("alarm"));
        assertEquals(START, event.get_time_fields().get("start"));
    }

    // Checks: set_field() rejects a field that events do not have.
    @Test
    void setField_fieldOfOtherType_throws() {
        assertThrows(InvalidFieldException.class, () -> newEvent().set_field("deadline", LATER));
    }

    // Checks: set_field() rejects an alarm that is not a valid date-time with
    // InvalidFieldException (plan 4.1), and the old alarm is kept.
    @Test
    void setField_invalidAlarm_throwsInvalidFieldException() {
        Event event = newEvent();

        assertThrows(InvalidFieldException.class, () -> event.set_field("alarm", "tomorrow"));

        assertEquals(ALARM, event.get_time_fields().get("alarm"));
    }

    // Checks: the constructor rejects a start time that is not a valid date-time
    // with InvalidFieldException (plan 4.1).
    @Test
    void constructor_invalidStart_throwsInvalidFieldException() {
        assertThrows(InvalidFieldException.class, () -> new Event(4, "Group meeting", "2pm", ALARM));
    }

    // Checks (US8): the detail string shows the ID, description, start and alarm.
    @Test
    void toDetailString_containsAllFields() {
        String details = newEvent().to_detail_string();

        assertTrue(details.contains("4"));
        assertTrue(details.contains("Group meeting"));
        assertTrue(details.contains(START));
        assertTrue(details.contains(ALARM));
    }

    // Checks (US10): to_record() gives the ID, type and every field, ready to be saved.
    @Test
    void toRecord_containsIdTypeAndFields() {
        Map<String, String> expected = Map.of(
                "id", "4", "type", "event", "description", "Group meeting", "start", START, "alarm", ALARM);

        assertEquals(expected, newEvent().to_record());
    }
}

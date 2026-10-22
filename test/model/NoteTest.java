package model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Note (US2: plain-text notes). A note has one text field, "text".
 */
class NoteTest {

    // Checks: a note reports its type as "note".
    @Test
    void getType_isNote() {
        assertEquals("note", new Note(1, "Buy milk").getType());
    }

    // Checks: the constructor stores the ID and the text.
    @Test
    void constructor_storesIdAndText() {
        Note note = new Note(1, "Buy milk");

        assertEquals(1, note.getId());
        assertEquals(Map.of("text", "Buy milk"), note.get_text_fields());
    }

    // Checks: a note has no time fields, so time-based searches never match it.
    @Test
    void getTimeFields_isEmpty() {
        assertTrue(new Note(1, "Buy milk").get_time_fields().isEmpty());
    }

    // Checks: the declared field names are exactly "text" (text) and none (time).
    @Test
    void fieldNames_matchSpecification() {
        Note note = new Note(1, "Buy milk");

        assertEquals(Set.of("text"), note.getTextFields());
        assertEquals(Set.of(), note.getTimeFields());
    }

    // Checks (US6): set_field() on "text" replaces the note's text.
    @Test
    void setField_text_updatesText() {
        Note note = new Note(1, "Buy milk");

        note.set_field("text", "Buy oat milk");

        assertEquals("Buy oat milk", note.get_text_fields().get("text"));
    }

    // Checks: set_field() rejects a field that notes do not have.
    @Test
    void setField_fieldOfOtherType_throws() {
        Note note = new Note(1, "Buy milk");

        assertThrows(InvalidFieldException.class, () -> note.set_field("deadline", "anything"));
    }

    // Checks (US8): the detail string shows the ID and the text.
    @Test
    void toDetailString_containsIdAndText() {
        String details = new Note(5, "Buy milk").to_detail_string();

        assertTrue(details.contains("5"));
        assertTrue(details.contains("Buy milk"));
    }

    // Checks (US10): to_record() gives the ID, type and every field, ready to be saved.
    @Test
    void toRecord_containsIdTypeAndFields() {
        Map<String, String> expected = Map.of("id", "5", "type", "note", "text", "Buy milk");

        assertEquals(expected, new Note(5, "Buy milk").to_record());
    }
}

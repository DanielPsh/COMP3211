package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for PIRRepository (US8, US9 and ID handling). */
class PIRRepositoryTest {

    private final PIRRepository repo = new PIRRepository();

    private Note addNote(String text) {
        Note note = new Note(repo.getNextId(), text);
        repo.add(note);
        return note;
    }

    // Checks: a new repository is empty and the first ID is 1.
    @Test
    void newRepository_isEmpty_firstIdIsOne() {
        assertTrue(repo.get_all().isEmpty());
        assertEquals(1, repo.getNextId());
    }

    // Checks: add returns the PIR's ID and get returns the same object.
    @Test
    void add_thenGet_returnsSamePir() {
        Note note = new Note(1, "Buy milk");
        assertEquals(1, repo.add(note));
        assertSame(note, repo.get(1));
        assertEquals(2, repo.getNextId());
    }

    // Checks: adding a second PIR with an ID already in use is rejected.
    @Test
    void add_duplicateId_throws() {
        repo.add(new Note(1, "a"));
        assertThrows(PIMException.class, () -> repo.add(new Note(1, "b")));
    }

    // Checks: get with an unknown ID throws PIRNotFoundException.
    @Test
    void get_unknownId_throws() {
        assertThrows(PIRNotFoundException.class, () -> repo.get(99));
    }

    // Checks: a deleted PIR can no longer be found, others stay.
    @Test
    void delete_removesOnlyThatPir() {
        addNote("a");
        addNote("b");
        repo.delete(1);
        assertThrows(PIRNotFoundException.class, () -> repo.get(1));
        assertEquals(1, repo.get_all().size());
        assertEquals("b", repo.get(2).get_text_fields().get("text"));
    }

    // Checks: deleting an unknown ID throws PIRNotFoundException.
    @Test
    void delete_unknownId_throws() {
        assertThrows(PIRNotFoundException.class, () -> repo.delete(5));
    }

    // Checks: IDs are never reused, even after the newest PIR is deleted.
    @Test
    void delete_doesNotReuseId() {
        addNote("a");
        addNote("b");
        repo.delete(2);
        assertEquals(3, repo.getNextId());
    }

    // Checks: get_all keeps insertion order and returns a copy.
    @Test
    void getAll_keepsOrder_andIsACopy() {
        addNote("a");
        addNote("b");
        List<PIR> all = repo.get_all();
        assertEquals(1, all.get(0).getId());
        assertEquals(2, all.get(1).getId());
        all.clear();
        assertEquals(2, repo.get_all().size());
    }

    // Checks: search returns only the PIRs the criterion matches.
    @Test
    void search_returnsMatchingPirs() {
        addNote("Buy milk");
        addNote("Call mom");
        List<PIR> found = repo.search(new TextContainsCriterion("text", "milk"));
        assertEquals(1, found.size());
        assertEquals(1, found.get(0).getId());
    }

    // Checks: search with no matches returns an empty list.
    @Test
    void search_noMatch_isEmpty() {
        addNote("Buy milk");
        assertTrue(repo.search(new TypeCriterion("task")).isEmpty());
    }

    // Checks: replace_all keeps the loaded IDs and continues after the largest one.
    @Test
    void replaceAll_keepsIds_andSetsNextId() {
        addNote("old");
        repo.replace_all(List.of(new Note(4, "x"), new Note(7, "y")));
        assertThrows(PIRNotFoundException.class, () -> repo.get(1));
        assertEquals("y", repo.get(7).get_text_fields().get("text"));
        assertEquals(8, repo.getNextId());
    }

    // Checks: replace_all with an empty list clears everything and resets IDs.
    @Test
    void replaceAll_empty_clears() {
        addNote("old");
        repo.replace_all(List.of());
        assertTrue(repo.get_all().isEmpty());
        assertEquals(1, repo.getNextId());
    }
}

package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Unit tests for saving and loading PIRs (US10 and US11). */
class PimFileStorageTest {
    private static final String HEADER = "PIMFILE v1";
    private static final String START = "2026-11-20 20:00:15";
    private static final String ALARM = "2026-11-20 19:45:05";

    @TempDir
    Path directory;

    // Checks that every PIR type keeps its fields, saved ID and order after reloading.
    @Test
    void roundTripPreservesAllRecordTypesFieldsIdsAndOrder() {
        Path path = directory.resolve("all records.pim");
        List<PIR> original = List.of(
                new Note(9, "Remember the charger"),
                new Task(3, "Submit project", START),
                new Event(6, "Team meeting", START, ALARM),
                new Contact(17, "陳大文", "Hung Hom", "00123456"));

        PimFileStorage.save(path.toString(), original);
        List<PIR> loaded = PimFileStorage.load(path.toString());

        assertEquals(original.stream().map(PIR::to_record).toList(),
                loaded.stream().map(PIR::to_record).toList());
        assertInstanceOf(Note.class, loaded.get(0));
        assertInstanceOf(Task.class, loaded.get(1));
        assertInstanceOf(Event.class, loaded.get(2));
        assertInstanceOf(Contact.class, loaded.get(3));
    }

    // Checks the bytes produced by save against the documented on-disk escaping.
    @Test
    void saveUsesVersionedTabSeparatedUtf8WithEscapedValues() throws IOException {
        Path path = directory.resolve("escaped.pim");
        String text = " café\tline one\nline two\rpath\\name=last ";
        PimFileStorage.save(path.toString(), List.of(new Note(5, text)));

        assertEquals(List.of(HEADER, "5\tnote\ttext= café\\tline one\\nline two\\rpath\\\\name=last "),
                Files.readAllLines(path));
        assertEquals(Map.of("text", text), PimFileStorage.load(path.toString()).getFirst().get_text_fields());
    }

    // Checks a separately authored v1 file, including CRLF, an empty field and an embedded equals sign.
    @Test
    void loadReadsAnIndependentV1File() throws IOException {
        Path path = directory.resolve("fixture.pim");
        Files.writeString(path, HEADER + "\r\n"
                + "4\tnote\ttext=\r\n"
                + "7\tcontact\tname=Kai=Lee\taddress=Room\\t102\tmobile=0007");

        List<PIR> loaded = PimFileStorage.load(path.toString());

        assertEquals(List.of(4, 7), loaded.stream().map(PIR::getId).toList());
        assertEquals(Map.of("text", ""), loaded.getFirst().get_text_fields());
        assertEquals(Map.of("name", "Kai=Lee", "address", "Room\t102", "mobile", "0007"),
                loaded.getLast().get_text_fields());
    }

    // Checks that saving an empty list replaces an existing file with a valid empty snapshot.
    @Test
    void emptySaveOverwritesOldRecordsWithHeaderOnly() throws IOException {
        Path path = directory.resolve("empty.pim");
        PimFileStorage.save(path.toString(), List.of(new Note(1, "old")));
        PimFileStorage.save(path.toString(), List.of());

        assertEquals(List.of(HEADER), Files.readAllLines(path));
        assertTrue(PimFileStorage.load(path.toString()).isEmpty());
    }

    // Checks that loading sparse IDs allows the repository to allocate the next ID after the largest.
    @Test
    void repositoryCanReplaceItsRecordsWithLoadedIds() {
        Path path = directory.resolve("restore.pim");
        PimFileStorage.save(path.toString(), List.of(new Note(15, "new"), new Note(4, "other")));
        PIRRepository repository = new PIRRepository();
        repository.add(new Note(1, "old"));

        repository.replace_all(PimFileStorage.load(path.toString()));

        assertThrows(PIRNotFoundException.class, () -> repository.get(1));
        assertEquals("new", repository.get(15).get_text_fields().get("text"));
        assertEquals(16, repository.getNextId());
    }

    // Checks that the largest ID is restored and the next ID cannot overflow.
    @Test
    void largestIntegerIdRoundTripsWithoutWrappingRepositoryIds() {
        Path path = directory.resolve("max-id.pim");
        PimFileStorage.save(path.toString(), List.of(new Note(Integer.MAX_VALUE, "maximum")));

        PIRRepository repository = new PIRRepository();
        repository.replace_all(PimFileStorage.load(path.toString()));

        assertEquals("maximum", repository.get(Integer.MAX_VALUE).get_text_fields().get("text"));
        assertThrows(PIMException.class, repository::getNextId);
    }

    // Checks that the extension rule applies on save and load even when file contents are valid.
    @ParameterizedTest
    @ValueSource(strings = {"backup", "backup.txt", "backup.PIM", "backup.pim.bak"})
    void invalidExtensionIsRejectedWithoutCreatingOrReadingFile(String fileName) throws IOException {
        Path path = directory.resolve(fileName);
        assertThrows(PimFileException.class,
                () -> PimFileStorage.save(path.toString(), List.of(new Note(1, "save attempt"))));
        assertFalse(Files.exists(path));

        Files.writeString(path, HEADER + "\n");
        assertThrows(PimFileException.class, () -> PimFileStorage.load(path.toString()));
    }

    // Checks that a missing input is reported without creating the file.
    @Test
    void loadMissingFileReportsStorageError() {
        Path path = directory.resolve("missing.pim");
        assertThrows(PimFileException.class, () -> PimFileStorage.load(path.toString()));
        assertFalse(Files.exists(path));
    }

    // Checks that a trailing directory separator cannot address a snapshot as a file.
    @Test
    void trailingSlashDoesNotOverwriteSnapshot() throws IOException {
        Path path = directory.resolve("existing.pim");
        Files.writeString(path, HEADER + "\n1\tnote\ttext=keep");
        String directoryPath = path + "/";

        assertThrows(PimFileException.class, () -> PimFileStorage.save(directoryPath, List.of()));
        assertThrows(PimFileException.class, () -> PimFileStorage.load(directoryPath));
        assertEquals(HEADER + "\n1\tnote\ttext=keep", Files.readString(path));
    }

    // Checks that invalid UTF-8 is rejected instead of becoming replacement characters in a record.
    @Test
    void loadRejectsMalformedUtf8() throws IOException {
        Path path = directory.resolve("bad-encoding.pim");
        byte[] contents = (HEADER + "\n1\tnote\ttext=?").getBytes(StandardCharsets.UTF_8);
        contents[contents.length - 1] = (byte) 0xFF;
        Files.write(path, contents);

        assertThrows(PimFileException.class, () -> PimFileStorage.load(path.toString()));
    }

    // Checks that malformed files fail as a whole, including rows following an otherwise valid record.
    @ParameterizedTest(name = "{0}")
    @MethodSource("corruptFiles")
    void loadRejectsCorruptFile(String description, String contents) throws IOException {
        Path path = directory.resolve("corrupt.pim");
        Files.writeString(path, contents);

        assertThrows(PimFileException.class, () -> PimFileStorage.load(path.toString()), description);
    }

    private static Stream<Arguments> corruptFiles() {
        String validPrefix = HEADER + "\n1\tnote\ttext=valid\n";
        return Stream.of(
                Arguments.of("empty file", ""),
                Arguments.of("wrong format version", "PIMFILE v2\n"),
                Arguments.of("missing field columns", validPrefix + "2\tnote"),
                Arguments.of("nonnumeric ID", validPrefix + "abc\tnote\ttext=x"),
                Arguments.of("overflowing ID", validPrefix + "2147483648\tnote\ttext=x"),
                Arguments.of("nonpositive ID", validPrefix + "0\tnote\ttext=x"),
                Arguments.of("duplicate ID", validPrefix + "1\tnote\ttext=x"),
                Arguments.of("unknown record type", validPrefix + "2\tmemo\ttext=x"),
                Arguments.of("missing required field", validPrefix + "2\tcontact\tname=Kai\taddress=Here"),
                Arguments.of("extra field", validPrefix + "2\tnote\ttext=x\tmobile=123"),
                Arguments.of("duplicate field", validPrefix + "2\tnote\ttext=x\ttext=y"),
                Arguments.of("missing equals separator", validPrefix + "2\tnote\ttext"),
                Arguments.of("unknown escape", validPrefix + "2\tnote\ttext=x\\q"),
                Arguments.of("unfinished escape", validPrefix + "2\tnote\ttext=x\\"),
                Arguments.of("invalid deadline", validPrefix + "2\ttask\tdescription=Submit\tdeadline=soon"),
                Arguments.of("impossible calendar date", validPrefix
                        + "2\ttask\tdescription=Submit\tdeadline=2026-02-30 12:00:00"));
    }

    // Checks that a failed save cannot replace a previously valid file with a partial snapshot.
    @Test
    void invalidRecordsLeaveExistingSnapshotIntact() throws IOException {
        Path path = directory.resolve("keep.pim");
        PimFileStorage.save(path.toString(), List.of(new Note(2, "keep this")));
        String previous = Files.readString(path);

        assertThrows(PimFileException.class, () -> PimFileStorage.save(path.toString(),
                List.of(new Note(7, "first"), new Note(7, "duplicate ID"))));

        assertEquals(previous, Files.readString(path));
    }

    // Checks that save reports a missing parent without creating unrequested directories.
    @Test
    void saveToMissingParentDoesNotCreateDirectories() {
        Path parent = directory.resolve("absent");
        Path path = parent.resolve("new.pim");

        assertThrows(PimFileException.class, () -> PimFileStorage.save(path.toString(), List.of()));
        assertFalse(Files.exists(parent));
    }

    // Checks that I/O failures are reported through the storage exception without removing a directory.
    @Test
    void directoryCannotBeLoadedOrOverwrittenAsAFile() throws IOException {
        Path path = Files.createDirectory(directory.resolve("directory.pim"));
        Path child = Files.writeString(path.resolve("keep.txt"), "keep");

        assertThrows(PimFileException.class, () -> PimFileStorage.load(path.toString()));
        assertThrows(PimFileException.class, () -> PimFileStorage.save(path.toString(), List.of()));
        assertEquals("keep", Files.readString(child));
    }
}

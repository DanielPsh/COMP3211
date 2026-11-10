package model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reads and writes the UTF-8 PIMFILE v1 snapshot format. */
public final class PimFileStorage {
    private static final String HEADER = "PIMFILE v1";

    private PimFileStorage() {
    }

    /** Replaces the destination only after the complete snapshot has been written. */
    public static void save(String path, List<PIR> pirs) throws PimFileException {
        Path target = pimPath(path).toAbsolutePath();
        Path staged = null;
        try {
            staged = Files.createTempFile(target.getParent(), ".pim-save-", ".tmp");
            try (BufferedWriter out = Files.newBufferedWriter(staged, StandardCharsets.UTF_8)) {
                out.write(HEADER);
                out.write('\n');
                Set<Integer> ids = new HashSet<>();
                for (PIR pir : pirs) {
                    writeRecord(out, pir, ids);
                }
            }
            Files.move(staged, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new PimFileException("Could not save '" + path + "': " + e.getMessage(), e);
        } finally {
            if (staged != null) {
                try {
                    Files.deleteIfExists(staged);
                } catch (IOException ignored) {
                    // Cleanup failure must not hide the original save error.
                }
            }
        }
    }

    /** Returns a fully validated snapshot in file order, with the original record IDs. */
    public static List<PIR> load(String path) throws PimFileException {
        Path source = pimPath(path);
        List<PIR> pirs = new ArrayList<>();
        Set<Integer> ids = new HashSet<>();
        try (BufferedReader in = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            if (!HEADER.equals(in.readLine())) {
                throw new PimFileException("Invalid .pim header in '" + path + "'. Expected " + HEADER + ".");
            }
            int lineNumber = 1;
            String line;
            while ((line = in.readLine()) != null) {
                lineNumber++;
                try {
                    pirs.add(readRecord(line, ids));
                } catch (PimFileException | InvalidFieldException | NumberFormatException e) {
                    throw new PimFileException("Invalid record in '" + path + "' at line " + lineNumber
                            + ": " + e.getMessage(), e);
                }
            }
        } catch (NoSuchFileException e) {
            throw new PimFileException("File '" + path + "' does not exist.", e);
        } catch (IOException e) {
            throw new PimFileException("Could not load '" + path + "': " + e.getMessage(), e);
        }
        return pirs;
    }

    private static Path pimPath(String path) {
        if (path == null || path.isEmpty()) {
            throw new PimFileException("A file path ending in .pim is required.");
        }
        if (!path.endsWith(".pim")) {
            throw new PimFileException("The file name must end in .pim.");
        }
        try {
            return Path.of(path);
        } catch (InvalidPathException e) {
            throw new PimFileException("Invalid file path: " + e.getReason(), e);
        }
    }

    private static void writeRecord(BufferedWriter out, PIR pir, Set<Integer> ids) throws IOException {
        checkId(pir.getId(), ids);
        List<String> required = requiredFields(pir.getType());
        Map<String, String> fields = pir.to_record();
        out.write(Integer.toString(pir.getId()));
        out.write('\t');
        out.write(pir.getType());
        for (String field : required) {
            String value = fields.get(field);
            if (value == null) {
                throw new PimFileException("Record " + pir.getId() + " is missing field '" + field + "'.");
            }
            out.write('\t');
            out.write(field);
            out.write('=');
            out.write(escape(value));
        }
        out.write('\n');
    }

    private static PIR readRecord(String line, Set<Integer> ids) {
        String[] columns = line.split("\t", -1);
        if (columns.length < 3) {
            throw new PimFileException("Expected an ID, a type, and field assignments.");
        }
        int id = Integer.parseInt(columns[0]);
        checkId(id, ids);
        String type = columns[1];
        List<String> required = requiredFields(type);
        Map<String, String> fields = new HashMap<>();
        for (int i = 2; i < columns.length; i++) {
            String column = columns[i];
            int separator = column.indexOf('=');
            if (separator <= 0) {
                throw new PimFileException("Expected a field assignment.");
            }
            String field = column.substring(0, separator);
            if (fields.putIfAbsent(field, unescape(column.substring(separator + 1))) != null) {
                throw new PimFileException("Duplicate field '" + field + "'.");
            }
        }
        if (fields.size() != required.size() || !fields.keySet().containsAll(required)) {
            throw new PimFileException("Expected fields " + String.join(", ", required) + " for " + type + ".");
        }
        PIR pir = PIRFactory.create(id, type, fields);
        for (Map.Entry<String, String> time : pir.get_time_fields().entrySet()) {
            // The model parser can normalize dates such as February 30. A snapshot must be exact.
            if (!time.getValue().equals(fields.get(time.getKey()))) {
                throw new PimFileException("Invalid date/time in field '" + time.getKey() + "'.");
            }
        }
        return pir;
    }

    private static List<String> requiredFields(String type) {
        List<String> fields = PIRFactory.requiredFields(type);
        if (fields.isEmpty()) {
            throw new PimFileException("Unknown record type '" + type + "'.");
        }
        return fields;
    }

    private static void checkId(int id, Set<Integer> ids) {
        if (id <= 0) {
            throw new PimFileException("Record IDs must be positive.");
        }
        if (!ids.add(id)) {
            throw new PimFileException("Duplicate record ID " + id + ".");
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String unescape(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\') {
                if (++i == value.length()) {
                    throw new PimFileException("Unfinished escape in field value.");
                }
                c = switch (value.charAt(i)) {
                    case '\\' -> '\\';
                    case 't' -> '\t';
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    default -> throw new PimFileException("Unknown escape in field value.");
                };
            }
            result.append(c);
        }
        return result.toString();
    }
}

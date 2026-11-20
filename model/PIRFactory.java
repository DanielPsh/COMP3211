package model;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PIRFactory {

    private static final Map<String, List<String>> REQUIRED_FIELDS = Map.of(
            "note", List.of("text"),
            "task", List.of("description", "deadline"),
            "event", List.of("description", "start", "alarm"),
            "contact", List.of("name", "address", "mobile"));

    public static final Set<String> SUPPORTED_TYPES = REQUIRED_FIELDS.keySet();

    private PIRFactory() {
    }

    public static List<String> requiredFields(String type) {
        return REQUIRED_FIELDS.getOrDefault(type, List.of());
    }

    public static PIR create(int id, String type, Map<String, String> fields) throws InvalidFieldException {
        if (type == null) {
            throw new InvalidFieldException("Record type is missing. Supported types: " + SUPPORTED_TYPES);
        }

        PIR pir;
        switch (type.toLowerCase()) {
            case "note":
                pir = new Note(id, fields.get("text"));
                break;
            case "task":
                pir = new Task(id, fields.get("description"), fields.get("deadline"));
                break;
            case "event":
                pir = new Event(id, fields.get("description"), fields.get("start"), fields.get("alarm"));
                break;
            case "contact":
                pir = new Contact(id, fields.get("name"), fields.get("address"), fields.get("mobile"));
                break;
            default:
                throw new InvalidFieldException(
                        "Unknown record type '" + type + "'. Supported types: " + SUPPORTED_TYPES);
        }

        for (String name : fields.keySet()) {
            pir.validateFieldName(name);
        }

        return pir;
    }
}

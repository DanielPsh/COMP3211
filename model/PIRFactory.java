package model;

import java.util.Map;
import java.util.Set;

public final class PIRFactory {

    public static final Set<String> SUPPORTED_TYPES = Set.of("note", "task", "event", "contact");

    private PIRFactory() {
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
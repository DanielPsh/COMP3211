package model;

import java.util.Map;
import java.util.Set;
import java.time.format.DateTimeFormatter;

public abstract class PIR {

    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    int id;

    abstract Set<String> getTextFields();

    abstract Set<String> getTimeFields();

    public PIR(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public abstract String getType();

    public void validateFieldName(String fieldName) {
        Set<String> textFields = getTextFields();
        Set<String> timeFields = getTimeFields();

        if (!textFields.contains(fieldName) && !timeFields.contains(fieldName)) {
            throw new InvalidFieldException(
                    "Field '" + fieldName + "' does not belong to record type '" + getType() + "'");
        }
    }

    public abstract void apply_fields(String name, String value);

    public void set_field(String name, String value) {
        validateFieldName(name);
        apply_fields(name, value);
    }

    public abstract Map<String, String> get_text_fields();

    public abstract Map<String, String> get_time_fields();

    public abstract String to_detail_string();

    public abstract Map<String, String> to_record();

}

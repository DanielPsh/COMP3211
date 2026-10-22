package model;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;

public class Note extends PIR {

    private static final Set<String> TEXT_FIELDS = Set.of("text");
    private static final Set<String> TIME_FIELDS = Set.of();

    @Override
    Set<String> getTextFields() {
        return TEXT_FIELDS;
    }

    @Override
    Set<String> getTimeFields() {
        return TIME_FIELDS;
    }

    private String text;

    public Note(int id, String text) {
        super(id);
        this.text = text;
    }

    @Override
    public String getType() {
        return "note";
    }

    @Override
    public Map<String, String> get_text_fields() {
        Map<String, String> fields = new HashMap<>();
        fields.put("text", this.text);
        return fields;
    }

    @Override
    public Map<String, String> get_time_fields() {
        return Map.of();
    }

    @Override
    public void apply_fields(String name, String value) {
        this.text = value;
    }

    @Override
    public String to_detail_string() {
        return "Note ID: " + getId() + "\n" +
                "Text: " + text;
    }

    @Override
    public Map<String, String> to_record() {
        Map<String, String> record = new HashMap<>();
        record.put("id", String.valueOf(getId()));
        record.put("type", getType());
        record.put("text", this.text);
        return record;
    }
}

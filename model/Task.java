package model;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.time.LocalDateTime;

public class Task extends PIR {

    private static final Set<String> TEXT_FIELDS = Set.of("description");
    private static final Set<String> TIME_FIELDS = Set.of("deadline");

    @Override
    Set<String> getTextFields() {
        return TEXT_FIELDS;
    }

    @Override
    Set<String> getTimeFields() {
        return TIME_FIELDS;
    }

    private String description;
    private LocalDateTime deadline;

    public Task(int id, String description, String deadline) {
        super(id);
        this.description = description;
        this.deadline = DateTimeUtil.parseDateTime(deadline, TIME_FORMAT);
    }

    @Override
    public String getType() {
        return "task";
    }

    @Override
    public Map<String, String> get_text_fields() {
        Map<String, String> fields = new HashMap<>();
        fields.put("description", this.description);
        return fields;
    }

    @Override
    public Map<String, String> get_time_fields() {
        Map<String, String> fields = new HashMap<>();
        fields.put("deadline", this.deadline.format(TIME_FORMAT));
        return fields;
    }

    @Override
    public void apply_fields(String name, String value) {
        switch (name) {
            case "description":
                this.description = value;
                break;
            case "deadline":
                this.deadline = DateTimeUtil.parseDateTime(value, TIME_FORMAT);
                break;
        }
    }

    @Override
    public String to_detail_string() {
        return "Task ID: " + getId() + "\n" +
                "Description: " + description + "\n" +
                "Deadline: " + deadline.format(TIME_FORMAT);
    }

    @Override
    public Map<String, String> to_record() {
        Map<String, String> record = new HashMap<>();
        record.put("id", String.valueOf(getId()));
        record.put("type", getType());
        record.put("description", this.description);
        record.put("deadline", this.deadline.format(TIME_FORMAT));
        return record;
    }
}
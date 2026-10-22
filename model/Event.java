package model;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.time.LocalDateTime;

public class Event extends PIR {

    private static final Set<String> TEXT_FIELDS = Set.of("description");
    private static final Set<String> TIME_FIELDS = Set.of("start", "alarm");

    @Override
    Set<String> getTextFields() {
        return TEXT_FIELDS;
    }

    @Override
    Set<String> getTimeFields() {
        return TIME_FIELDS;
    }

    private String description;
    private LocalDateTime start;
    private LocalDateTime alarm;

    public Event(int id, String description, String start, String alarm) {
        super(id);
        this.description = description;
        this.start = DateTimeUtil.parseDateTime(start, TIME_FORMAT);
        this.alarm = DateTimeUtil.parseDateTime(alarm, TIME_FORMAT);
    }

    @Override
    public String getType() {
        return "event";
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
        fields.put("start", this.start.format(TIME_FORMAT));
        fields.put("alarm", this.alarm.format(TIME_FORMAT));
        return fields;
    }

    @Override
    public void apply_fields(String name, String value) {
        switch (name) {
            case "description":
                this.description = value;
                break;
            case "start":
                this.start = DateTimeUtil.parseDateTime(value, TIME_FORMAT);
                break;
            case "alarm":
                this.alarm = DateTimeUtil.parseDateTime(value, TIME_FORMAT);
                break;
        }
    }

    @Override
    public String to_detail_string() {
        return "Event ID: " + getId() + "\n" +
                "Description: " + description + "\n" +
                "Start: " + start.format(TIME_FORMAT) + "\n" +
                "Alarm: " + alarm.format(TIME_FORMAT);
    }

    @Override
    public Map<String, String> to_record() {
        Map<String, String> record = new HashMap<>();
        record.put("id", String.valueOf(getId()));
        record.put("type", getType());
        record.put("description", this.description);
        record.put("start", this.start.format(TIME_FORMAT));
        record.put("alarm", this.alarm.format(TIME_FORMAT));
        return record;
    }

}

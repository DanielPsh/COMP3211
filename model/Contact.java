package model;

import java.util.Map;
import java.util.Set;

public class Contact extends PIR {

    private static final Set<String> TEXT_FIELDS = Set.of("name", "address", "mobile");
    private static final Set<String> TIME_FIELDS = Set.of();

    @Override
    Set<String> getTextFields() {
        return TEXT_FIELDS;
    }

    @Override
    Set<String> getTimeFields() {
        return TIME_FIELDS;
    }

    private String name;
    private String address;
    private String mobile;

    public Contact(int id, String name, String address, String mobile) {
        super(id);
        this.name = name;
        this.address = address;
        this.mobile = mobile;
    }

    @Override
    public String getType() {
        return "contact";
    }

    @Override
    public Map<String, String> get_text_fields() {
        return Map.of(
                "name", this.name,
                "address", this.address,
                "mobile", this.mobile);
    }

    @Override
    public Map<String, String> get_time_fields() {
        return Map.of();
    }

    @Override
    public void apply_fields(String name, String value) {
        switch (name) {
            case "name":
                this.name = value;
                break;
            case "address":
                this.address = value;
                break;
            case "mobile":
                this.mobile = value;
                break;
        }
    }

    @Override
    public String to_detail_string() {
        return "Contact ID: " + getId() + "\n" +
                "Name: " + name + "\n" +
                "Address: " + address + "\n" +
                "Mobile: " + mobile;
    }

    @Override
    public Map<String, String> to_record() {
        return Map.of(
                "id", String.valueOf(getId()),
                "type", getType(),
                "name", this.name,
                "address", this.address,
                "mobile", this.mobile);
    }

}

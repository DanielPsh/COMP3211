package controller;

import java.util.List;
import java.util.Map;

public class Command {
    public final String name;
    public final List<String> words;
    public final Map<String, String> fields;
    public final String rest;

    public Command(String name, List<String> words, Map<String, String> fields, String rest) {
        this.name = name;
        this.words = words;
        this.fields = fields;
        this.rest = rest;
    }
}

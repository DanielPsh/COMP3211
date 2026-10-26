package controller;

import java.util.List;
import java.util.Map;

/** One parsed input line, e.g. create task description="x" deadline="...". */
public class Command {
    public final String name;               // "create", "print", ...
    public final List<String> words;        // plain words after the name, e.g. ["task"] or ["3"]
    public final Map<String, String> fields; // name="value" pairs
    public final String rest;               // raw text after the name (used by search)

    public Command(String name, List<String> words, Map<String, String> fields, String rest) {
        this.name = name;
        this.words = words;
        this.fields = fields;
        this.rest = rest;
    }
}

package controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.CriterionParser;
import model.PIMException;
import model.PIR;
import model.PIRFactory;
import model.PIRRepository;
import model.PimFileStorage;

/** Runs one command at a time and returns the text to show. Never prints. */
public class PIMController {

    private final PIRRepository repository;
    private boolean running = true;

    public PIMController(PIRRepository repository) {
        this.repository = repository;
    }

    public boolean isRunning() {
        return running;
    }

    /** Runs one input line. Errors are returned as messages, never thrown. */
    public String execute(String line) {
        try {
            return run(CommandParser.parse(line));
        } catch (PIMException e) {
            return "Error: " + e.getMessage();
        } catch (RuntimeException e) {
            return "Error: missing or invalid value (" + e.getMessage() + ")";
        } catch (StackOverflowError e) {
            return "Error: the command is too long or too complex to run.";
        }
    }

    private String run(Command cmd) {
        switch (cmd.name) {
            case "create":
                return create(cmd);
            case "modify":
                return modify(cmd);
            case "search":
                return search(cmd);
            case "print":
                return print(cmd);
            case "delete":
                return delete(cmd);
            case "save":
                return save(cmd);
            case "load":
                return load(cmd);
            case "help":
                return helpText();
            case "exit":
                running = false;
                return "Bye.";
            default:
                throw new InvalidCommandException(
                        "Unknown command '" + cmd.name + "'. Type 'help' for a list of commands.");
        }
    }

    // create TYPE field="value" ...   (US1-US5)
    private String create(Command cmd) {
        if (cmd.words.size() != 1) {
            throw new InvalidCommandException("Usage: create <note|task|event|contact> field=\"value\" ...");
        }
        String type = cmd.words.get(0).toLowerCase();
        List<String> required = PIRFactory.requiredFields(type);
        for (String name : required) {
            if (!cmd.fields.containsKey(name)) {
                throw new InvalidCommandException("Missing field '" + name + "'. A " + type
                        + " needs: " + String.join(", ", required) + ".");
            }
        }
        PIR pir = PIRFactory.create(repository.getNextId(), type, cmd.fields);
        int id = repository.add(pir);
        return "Created " + pir.getType() + " with ID " + id + ".";
    }

    // modify ID field="value" ...   (US6)
    private String modify(Command cmd) {
        if (cmd.words.size() != 1 || cmd.fields.isEmpty()) {
            throw new InvalidCommandException("Usage: modify <id> field=\"value\" ...");
        }
        PIR pir = repository.get(parseId(cmd.words.get(0)));
        // Build a throwaway copy with the new values first, so a bad name or value
        // is reported before anything changes.
        Map<String, String> updated = new HashMap<>(pir.to_record());
        updated.remove("id");
        updated.remove("type");
        for (String name : cmd.fields.keySet()) {
            pir.validateFieldName(name);
            updated.put(name, cmd.fields.get(name));
        }
        PIRFactory.create(pir.getId(), pir.getType(), updated);
        for (String name : cmd.fields.keySet()) {
            pir.set_field(name, cmd.fields.get(name));
        }
        return "Modified record " + pir.getId() + ".";
    }

    // search EXPRESSION   (US7)
    private String search(Command cmd) {
        List<PIR> found = repository.search(CriterionParser.parse(cmd.rest));
        if (found.isEmpty()) {
            return "No records found.";
        }
        return found.size() + " record(s) found:\n\n" + details(found);
    }

    // print ID | print all   (US8)
    private String print(Command cmd) {
        if (cmd.words.size() != 1 || !cmd.fields.isEmpty()) {
            throw new InvalidCommandException("Usage: print <id> | print all");
        }
        if (cmd.words.get(0).equalsIgnoreCase("all")) {
            List<PIR> all = repository.get_all();
            return all.isEmpty() ? "No records." : details(all);
        }
        return repository.get(parseId(cmd.words.get(0))).to_detail_string();
    }

    // delete ID   (US9)
    private String delete(Command cmd) {
        if (cmd.words.size() != 1 || !cmd.fields.isEmpty()) {
            throw new InvalidCommandException("Usage: delete <id>");
        }
        int id = parseId(cmd.words.get(0));
        repository.delete(id);
        return "Deleted record " + id + ".";
    }

    // save PATH.pim   (US10)
    private String save(Command cmd) {
        String path = storagePath(cmd);
        List<PIR> records = repository.get_all();
        PimFileStorage.save(path, records);
        return "Saved " + records.size() + " record(s) to " + path + ".";
    }

    // load PATH.pim   (US11)
    private String load(Command cmd) {
        String path = storagePath(cmd);
        List<PIR> records = PimFileStorage.load(path);
        repository.replace_all(records);
        return "Loaded " + records.size() + " record(s) from " + path + ".";
    }

    private static String storagePath(Command cmd) {
        if (cmd.words.size() != 1) {
            throw new InvalidCommandException("Usage: " + cmd.name + " <file.pim>");
        }
        return cmd.words.get(0);
    }

    private static int parseId(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new InvalidCommandException("'" + text + "' is not a valid ID.");
        }
    }

    private static String details(List<PIR> pirs) {
        StringBuilder sb = new StringBuilder();
        for (PIR pir : pirs) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append(pir.to_detail_string());
        }
        return sb.toString();
    }

    private static String helpText() {
        return String.join("\n",
                "Commands:",
                "  create note text=\"...\"",
                "  create task description=\"...\" deadline=\"yyyy-MM-dd HH:mm:ss\"",
                "  create event description=\"...\" start=\"yyyy-MM-dd HH:mm:ss\" alarm=\"yyyy-MM-dd HH:mm:ss\"",
                "  create contact name=\"...\" address=\"...\" mobile=\"...\"",
                "  modify <id> field=\"value\" ...",
                "  search <expression>   e.g. search type = task && deadline < \"2026-11-20 00:00:00\"",
                "  print <id> | print all",
                "  delete <id>",
                "  save <file.pim> | load <file.pim>",
                "    Use double quotes around paths with spaces. Load replaces all current records.",
                "  help | exit");
    }
}

package model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PIRRepository {

    private final Map<Integer, PIR> pirs = new LinkedHashMap<>();
    private long nextId = 1;

    public int getNextId() {
        if (nextId > Integer.MAX_VALUE) {
            throw new PIMException("No record IDs are available in this collection.");
        }
        return (int) nextId;
    }

    public int add(PIR pir) {
        if (pirs.containsKey(pir.getId())) {
            throw new PIMException("ID " + pir.getId() + " is already in use.");
        }
        pirs.put(pir.getId(), pir);
        nextId = Math.max(nextId, (long) pir.getId() + 1);
        return pir.getId();
    }

    public PIR get(int id) {
        PIR pir = pirs.get(id);
        if (pir == null) {
            throw new PIRNotFoundException(id);
        }
        return pir;
    }

    public void delete(int id) {
        if (pirs.remove(id) == null) {
            throw new PIRNotFoundException(id);
        }
    }

    public List<PIR> get_all() {
        return new ArrayList<>(pirs.values());
    }

    public List<PIR> search(Criterion c) {
        List<PIR> result = new ArrayList<>();
        for (PIR pir : pirs.values()) {
            if (c.matches(pir)) {
                result.add(pir);
            }
        }
        return result;
    }

    public void replace_all(List<PIR> newPirs) {
        pirs.clear();
        nextId = 1;
        for (PIR pir : newPirs) {
            add(pir);
        }
    }
}

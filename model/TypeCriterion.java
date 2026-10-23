package model;

/** Matches PIRs of one type, e.g. {@code type = task}. */
public class TypeCriterion implements Criterion {
    private final String type;

    public TypeCriterion(String type) {
        this.type = type;
    }

    @Override
    public boolean matches(PIR pir) {
        return type.equals(pir.getType());
    }

    @Override
    public String toString() {
        return "type = " + type;
    }
}

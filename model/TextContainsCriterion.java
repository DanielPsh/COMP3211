package model;

/**
 * Matches PIRs whose text field contains a string (case-sensitive).
 * A PIR that does not have the field never matches.
 */
public class TextContainsCriterion implements Criterion {
    private final String field;
    private final String substring;

    public TextContainsCriterion(String field, String substring) {
        this.field = field;
        this.substring = substring;
    }

    @Override
    public boolean matches(PIR pir) {
        String value = pir.get_text_fields().get(field);
        return value != null && value.contains(substring);
    }

    @Override
    public String toString() {
        return field + " contains \"" + substring + "\"";
    }
}

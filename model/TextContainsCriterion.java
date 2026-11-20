package model;

import java.util.Locale;

/**
 * Matches PIRs whose text field contains a string, ignoring letter case.
 * A PIR that does not have the field never matches.
 */
public class TextContainsCriterion implements Criterion {
    private final String field;
    private final String substring;
    private final String lowerCaseSubstring;

    public TextContainsCriterion(String field, String substring) {
        this.field = field;
        this.substring = substring;
        this.lowerCaseSubstring = substring.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean matches(PIR pir) {
        String value = pir.get_text_fields().get(field);
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerCaseSubstring);
    }

    @Override
    public String toString() {
        return field + " contains \"" + substring + "\"";
    }
}

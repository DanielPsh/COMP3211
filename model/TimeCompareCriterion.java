package model;

import java.time.LocalDateTime;

/**
 * Compares a time field with a given point in time (before, after or equal).
 * A PIR that does not have the field never matches. PIRs hand out time fields
 * as text in {@link PIR#TIME_FORMAT}, which is parsed here before comparing.
 */
public class TimeCompareCriterion implements Criterion {
    public enum Comparison {
        BEFORE("<"), AFTER(">"), EQUAL("=");

        private final String symbol;

        Comparison(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    private final String field;
    private final Comparison comparison;
    private final LocalDateTime time;

    public TimeCompareCriterion(String field, Comparison comparison, LocalDateTime time) {
        this.field = field;
        this.comparison = comparison;
        this.time = time;
    }

    @Override
    public boolean matches(PIR pir) {
        String text = pir.get_time_fields().get(field);
        if (text == null) {
            return false;
        }
        LocalDateTime value = LocalDateTime.parse(text, PIR.TIME_FORMAT);
        switch (comparison) {
            case BEFORE: return value.isBefore(time);
            case AFTER: return value.isAfter(time);
            default: return value.isEqual(time);
        }
    }

    @Override
    public String toString() {
        return field + " " + comparison.getSymbol() + " " + time;
    }
}

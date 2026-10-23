package model;

/** The wrapped condition must not hold ({@code !}). */
public class NotCriterion implements Criterion {
    private final Criterion inner;

    public NotCriterion(Criterion inner) {
        this.inner = inner;
    }

    @Override
    public boolean matches(PIR pir) {
        return !inner.matches(pir);
    }

    @Override
    public String toString() {
        return "!" + inner;
    }
}

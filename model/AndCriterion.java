package model;

/** Both conditions must hold ({@code &&}). */
public class AndCriterion implements Criterion {
    private final Criterion left;
    private final Criterion right;

    public AndCriterion(Criterion left, Criterion right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean matches(PIR pir) {
        return left.matches(pir) && right.matches(pir);
    }

    @Override
    public String toString() {
        return "(" + left + " && " + right + ")";
    }
}

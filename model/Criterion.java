package model;

/** A search condition that a PIR either satisfies or does not (US7). */
public interface Criterion {
    boolean matches(PIR pir);
}

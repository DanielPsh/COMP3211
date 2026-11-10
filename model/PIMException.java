package model;

/** Base class for every error the PIM shows to the user. */
public class PIMException extends RuntimeException {
    public PIMException(String message) {
        super(message);
    }

    protected PIMException(String message, Throwable cause) {
        super(message, cause);
    }
}

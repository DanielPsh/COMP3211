package model;

public class PIMException extends RuntimeException {
    public PIMException(String message) {
        super(message);
    }

    protected PIMException(String message, Throwable cause) {
        super(message, cause);
    }
}

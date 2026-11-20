package model;

public class PimFileException extends PIMException {
    public PimFileException(String message) {
        super(message);
    }

    public PimFileException(String message, Throwable cause) {
        super(message, cause);
    }
}

package model;

/** Reports an invalid or inaccessible .pim file. */
public class PimFileException extends PIMException {
    public PimFileException(String message) {
        super(message);
    }

    public PimFileException(String message, Throwable cause) {
        super(message, cause);
    }
}

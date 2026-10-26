package model;

/** Thrown when no PIR has the requested ID (US8, US9). */
public class PIRNotFoundException extends PIMException {
    public PIRNotFoundException(int id) {
        super("No record with ID " + id + ".");
    }
}

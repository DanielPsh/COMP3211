package model;

public class PIRNotFoundException extends PIMException {
    public PIRNotFoundException(int id) {
        super("No record with ID " + id + ".");
    }
}

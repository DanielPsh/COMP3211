package controller;

import model.PIMException;

/** Thrown when the user types a command the PIM does not understand. */
public class InvalidCommandException extends PIMException {
    public InvalidCommandException(String message) {
        super(message);
    }
}

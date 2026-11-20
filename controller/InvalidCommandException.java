package controller;

import model.PIMException;

public class InvalidCommandException extends PIMException {
    public InvalidCommandException(String message) {
        super(message);
    }
}

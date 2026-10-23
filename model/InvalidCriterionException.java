package model;

/**
 * Thrown when a search expression cannot be parsed.
 * Unchecked, like Part A's InvalidFieldException. When the shared PIMException
 * base class exists, make this extend it.
 */
public class InvalidCriterionException extends RuntimeException {
    public InvalidCriterionException(String message) {
        super(message);
    }
}

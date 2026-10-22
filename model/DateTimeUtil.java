package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateTimeUtil {

    private DateTimeUtil() {
        // Private constructor to prevent instantiation
    }

    public static LocalDateTime parseDateTime(String dateTimeStr, DateTimeFormatter formatter) {
        try {
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (DateTimeParseException e) {
            throw new InvalidFieldException("Invalid date/time format: " + dateTimeStr);
        }
    }
}

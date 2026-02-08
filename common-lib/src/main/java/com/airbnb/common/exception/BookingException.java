package com.airbnb.common.exception;

import java.util.Map;

/**
 * Exception for booking-related errors
 */
public class BookingException extends AirbnbException {
    public BookingException(String errorCode, String message) {
        super(errorCode, message);
    }

    public BookingException(String errorCode, String message, Map<String, Object> context) {
        super(errorCode, message, context);
    }
}

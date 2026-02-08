package com.airbnb.common.exception;

import java.util.Map;

/**
 * Exception for availability-related errors
 */
public class AvailabilityException extends AirbnbException {
    public AvailabilityException(String errorCode, String message) {
        super(errorCode, message);
    }

    public AvailabilityException(String errorCode, String message, Map<String, Object> context) {
        super(errorCode, message, context);
    }
}

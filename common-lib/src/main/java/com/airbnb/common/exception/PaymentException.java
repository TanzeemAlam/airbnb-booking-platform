package com.airbnb.common.exception;

import java.util.Map;

/**
 * Exception for payment-related errors
 */
public class PaymentException extends AirbnbException {
    public PaymentException(String errorCode, String message) {
        super(errorCode, message);
    }

    public PaymentException(String errorCode, String message, Map<String, Object> context) {
        super(errorCode, message, context);
    }
}

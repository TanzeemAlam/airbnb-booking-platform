package com.airbnb.common.util;

import java.io.Serializable;
import java.util.Map;

/**
 * Standard error response for REST APIs
 * Java Record for immutability
 */
public record ErrorResponse (String errorCode, String message, Map<String, Object> context) implements Serializable {
    public ErrorResponse(String errorCode, String message) {
        this(errorCode, message, Map.of());
    }
}
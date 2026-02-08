package com.airbnb.common.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Base exception for Airbnb platform
 * Custom exception hierarchy with error codes and context
 */
public class AirbnbException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> context;

    public AirbnbException(String errorCode, String message) {
        this(errorCode, message, new HashMap<>());
    }

    public AirbnbException(String errorCode, String message, Map<String, Object> context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context != null ? context : new HashMap<>();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public AirbnbException addContext(String key, Object value) {
        this.context.put(key, value);
        return this;
    }
}

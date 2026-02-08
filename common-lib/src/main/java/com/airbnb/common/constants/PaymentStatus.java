package com.airbnb.common.constants;

/**
 * Payment status constants
 */
public final class PaymentStatus {
    public static final String PENDING = "PENDING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String REFUNDED = "REFUNDED";

    private PaymentStatus() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}

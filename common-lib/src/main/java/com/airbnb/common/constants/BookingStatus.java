package com.airbnb.common.constants;

/**
 * Booking status constants
 */
public final class BookingStatus {
    public static final String PENDING = "PENDING";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String CANCELLED = "CANCELLED";
    public static final String COMPLETED = "COMPLETED";

    private BookingStatus() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}

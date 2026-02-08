package com.airbnb.common.events;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Event published when booking is cancelled
 * Java Record for immutability and pattern matching support
 */
public record BookingCancelledEvent(
    Long bookingId,
    String reason,
    BigDecimal refundAmount
) implements Serializable {
    public BookingCancelledEvent {
        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException("Booking ID must be positive");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason cannot be empty");
        }
        if (refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Refund amount cannot be negative");
        }
    }
}

package com.airbnb.common.events;

import java.io.Serializable;

/**
 * Event published when payment fails
 * Java Record for immutability and pattern matching support
 */
public record PaymentFailedEvent(
    Long paymentId,
    Long bookingId,
    String reason
) implements Serializable {
    public PaymentFailedEvent {
        if (paymentId == null || paymentId <= 0) {
            throw new IllegalArgumentException("Payment ID must be positive");
        }
        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException("Booking ID must be positive");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Reason cannot be empty");
        }
    }
}

package com.airbnb.common.events;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Event published when payment succeeds
 * Java Record for immutability and pattern matching support
 */
public record PaymentSuccessEvent(
    Long paymentId,
    Long bookingId,
    BigDecimal amount,
    String stripeTransactionId
) implements Serializable {
    public PaymentSuccessEvent {
        if (paymentId == null || paymentId <= 0) {
            throw new IllegalArgumentException("Payment ID must be positive");
        }
        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException("Booking ID must be positive");
        }
    }
}

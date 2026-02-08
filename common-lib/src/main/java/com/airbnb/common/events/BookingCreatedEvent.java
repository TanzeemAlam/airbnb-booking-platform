package com.airbnb.common.events;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Event published when a booking is created
 * Java Record for immutability and pattern matching support
 */
public record BookingCreatedEvent(
    Long bookingId,
    Long listingId,
    Long userId,
    BigDecimal totalPrice,
    String currency
) implements Serializable {
    public BookingCreatedEvent {
        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException("Booking ID must be positive");
        }
        if (totalPrice == null || totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total price must be greater than zero");
        }
    }
}

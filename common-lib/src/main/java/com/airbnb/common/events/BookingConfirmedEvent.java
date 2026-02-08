package com.airbnb.common.events;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Event published when booking is confirmed
 * Java Record for immutability and pattern matching support
 */
public record BookingConfirmedEvent(
    Long bookingId,
    Long userId,
    LocalDate checkInDate,
    LocalDate checkOutDate
) implements Serializable {
    public BookingConfirmedEvent {
        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException("Booking ID must be positive");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalArgumentException("Check-in and check-out dates cannot be null");
        }
    }
}

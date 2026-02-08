package com.airbnb.common.dto;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO for Booking entity
 * Java Record for immutability and memory efficiency
 */
public record BookingDTO(
    Long id,
    Long userId,
    Long listingId,
    LocalDate checkInDate,
    LocalDate checkOutDate,
    String status
) implements Serializable {
    public BookingDTO {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        if (listingId == null || listingId <= 0) {
            throw new IllegalArgumentException("Listing ID must be positive");
        }
        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalArgumentException("Check-in and check-out dates cannot be null");
        }
        if (checkOutDate.isBefore(checkInDate) || checkOutDate.isEqual(checkInDate)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }
    }
}

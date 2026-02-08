package com.airbnb.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for Listing entity
 * Java Record for immutability and memory efficiency
 */
public record ListingDTO(
    Long id,
    String title,
    String description,
    String location,
    BigDecimal price,
    Long hostId,
    String imageUrl
) implements Serializable {
    public ListingDTO {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }
        if (hostId == null || hostId <= 0) {
            throw new IllegalArgumentException("Host ID must be positive");
        }
    }
}

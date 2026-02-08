package com.airbnb.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for Payment entity
 * Java Record for immutability and memory efficiency
 */
public record PaymentDTO(
    Long id,
    Long bookingId,
    BigDecimal amount,
    String currency,
    String status,
    String stripeTransactionId
) implements Serializable {
    public PaymentDTO {
        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException("Booking ID must be positive");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency cannot be empty");
        }
    }
}

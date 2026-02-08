package com.airbnb.common.dto;

import java.io.Serializable;

/**
 * DTO for Notification entity
 * Java Record for immutability and memory efficiency
 */
public record NotificationDTO(
    Long id,
    Long userId,
    String message,
    String type,
    boolean read
) implements Serializable {
    public NotificationDTO {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Type cannot be empty");
        }
    }
}

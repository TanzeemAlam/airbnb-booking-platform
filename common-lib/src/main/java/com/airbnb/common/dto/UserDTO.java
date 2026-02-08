package com.airbnb.common.dto;

import java.io.Serializable;

/**
 * DTO for User entity
 * Java Record for immutability and memory efficiency
 */
public record UserDTO(
    Long id,
    String email,
    String firstName,
    String lastName,
    String role
) implements Serializable {
    public UserDTO {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Role cannot be empty");
        }
    }
}

package com.airbnb.user.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * JwtBlacklist Entity - Stores revoked/blacklisted JWT tokens
 * 
 * Purpose: Implement logout functionality by marking tokens as invalid
 * When user logs out, their JWT is added here
 * During request processing, check if token is in blacklist before accepting
 * 
 * TODO: Add scheduled task to delete expired blacklist entries
 * TODO: Add method to check if token is blacklisted
 */
@Entity
@Table(name = "jwt_blacklist", indexes = {
    @Index(name = "idx_token", columnList = "token", unique = true),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_expiry_time", columnList = "expiry_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class JwtBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, columnDefinition = "LONGTEXT")
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expiry_time", nullable = false)
    private LocalDateTime expiryTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // TODO: Add pre-persist hook to set createdAt = now()
    // TODO: Add method: isExpired() -> LocalDateTime.now().isAfter(expiryTime)
    // TODO: Add static factory: ofToken(token, userId, expiryTime)
}

package com.airbnb.user.repository;

import com.airbnb.user.entity.JwtBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * JwtBlacklistRepository - Data access layer for JwtBlacklist entity
 * 
 * Manages revoked JWT tokens (logout, session termination)
 * 
 * Operations:
 *   - Add token to blacklist (logout)
 *   - Check if token is blacklisted (before processing request)
 *   - Delete expired tokens (cleanup)
 *   - Find all tokens for a user (session management)
 * 
 * TODO: Implement all method bodies
 */
@Repository
public interface JwtBlacklistRepository extends JpaRepository<JwtBlacklist, String> {

    /**
     * Find blacklist entry by token
     * Used to check if token is revoked during request processing
     */
    Optional<JwtBlacklist> findByToken(String token);

    /**
     * Check if token is blacklisted
     * Used in JWT validation filter
     */
    boolean existsByToken(String token);

    /**
     * Find all blacklisted tokens for a user
     * Used when user logs out from all devices
     */
    long countByUserId(Long userId);

    /**
     * Delete expired tokens
     * Used in scheduled cleanup job (runs daily)
     */
    @Modifying
    @Query(value = "DELETE FROM jwt_blacklist WHERE expiry_time < NOW()", nativeQuery = true)
    void deleteExpiredTokens();

    /**
     * Find tokens that expire soon
     * Used to notify user of upcoming session expiry
     */
    @Query(value = "SELECT * FROM jwt_blacklist " +
            "WHERE user_id = :userId " +
            "AND expiry_time > NOW() " +
            "AND expiry_time <= DATE_ADD(NOW(), INTERVAL 1 HOUR)",
            nativeQuery = true)
    java.util.List<JwtBlacklist> findExpiringTokensSoon(@Param("userId") Long userId);

    // TODO: Add more queries:
    // TODO: - Find all active (not expired) tokens for a user
    // TODO: - Check if user has any active sessions
    // TODO: - Delete all tokens for a user (force logout)
    // TODO: - Find recently blacklisted tokens (for audit)
}

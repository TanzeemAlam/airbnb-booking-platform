package com.airbnb.user.repository;

import com.airbnb.user.entity.UserAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UserAuditLogRepository - Data access layer for UserAuditLog entity
 * 
 * Tracks all user actions for security auditing:
 *   - Login attempts (success/failure)
 *   - Password changes
 *   - Profile updates
 *   - Permission changes
 *   - Failed authentication attempts
 * 
 * TODO: Implement all method bodies
 */
@Repository
public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, Long> {

    /**
     * Find all audit logs for a specific user
     * Used to review user's activity history
     */
    List<UserAuditLog> findByUserId(Long userId);

    /**
     * Find audit logs for a user within date range
     * Used for security investigations
     */
    List<UserAuditLog> findByUserIdAndCreatedAtBetween(
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );

    /**
     * Find failed login attempts for a user
     * Used to detect brute force attacks
     */
    List<UserAuditLog> findByUserIdAndIsSuccessfulFalse(Long userId);

    /**
     * Find recent activity for a user (paginated)
     * Used in activity dashboard
     */
    Page<UserAuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find failed attempts in last hour
     * Used for rate limiting decisions
     */
    @Query(value = "SELECT * FROM user_audit_logs " +
            "WHERE user_id = :userId " +
            "AND is_successful = false " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR)",
            nativeQuery = true)
    List<UserAuditLog> findRecentFailedAttempts(@Param("userId") Long userId);

    // TODO: Add more custom queries:
    // TODO: - Find all login attempts (successful and failed)
    // TODO: - Find suspicious activity patterns (multiple failures from different IPs)
    // TODO: - Find audit logs by action type
    // TODO: - Count failed attempts for rate limiting
    // TODO: - Find logs by IP address (track anomalies)
}

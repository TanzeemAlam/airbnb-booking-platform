package com.airbnb.user.entity;

import com.airbnb.common.entity.BaseEntity;
import lombok.*;

import javax.persistence.*;

/**
 * UserAuditLog Entity - Audit trail for user actions
 * 
 * Purpose: Log login attempts, logouts, password changes, etc.
 * Used for security monitoring and compliance
 * 
 * TODO: Add scheduled task to clean old logs (older than 90 days)
 * TODO: Add method to log login attempts with IP & User-Agent
 */
@Entity
@Table(name = "user_audit_logs", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserAuditLog extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "action", nullable = false)
    private String action; // LOGIN, LOGOUT, PASSWORD_CHANGE, EMAIL_CHANGE, etc.

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "is_successful", nullable = false)
    private Boolean isSuccessful;

    @Column(name = "failure_reason", length = 500)
    private String failureReason; // e.g., "Invalid credentials", "Account locked"

    // TODO: Add static factory method: ofLoginAttempt(userId, ipAddress, userAgent, success, reason)
    // TODO: Add static factory method: ofPasswordChange(userId, ipAddress)
    // TODO: Add method: wasSuccessful() -> return isSuccessful
}

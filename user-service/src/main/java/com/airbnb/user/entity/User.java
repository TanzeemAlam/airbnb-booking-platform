package com.airbnb.user.entity;

import com.airbnb.common.entity.BaseEntity;
import com.airbnb.user.enums.UserRole;
import lombok.*;

import javax.persistence.*;

/**
 * User Entity - Represents a user in the system
 * 
 * Fields:
 *   - email: Unique email address
 *   - password: BCrypt encoded password (NEVER store plaintext)
 *   - firstName, lastName: User's name
 *   - role: GUEST, HOST, or ADMIN
 *   - phoneNumber: Optional contact number
 *   - profileImageUrl: Optional profile picture
 *   - isVerified: Email verification status
 *   - isActive: Account active status (soft delete)
 * 
 * TODO: Add custom validators for email format, password strength
 * TODO: Implement equals/hashCode based on email
 * TODO: Add ToString with password masking
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_email", columnList = "email", unique = true),
    @Index(name = "idx_is_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "email", callSuper = false)
@ToString(exclude = "password") // Don't expose password in logs
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // TODO: Implement custom validation methods
    // TODO: Add method: getFullName() -> firstName + " " + lastName
    // TODO: Add method: isGuestRole(), isHostRole(), isAdminRole() for role checking
    // TODO: Add method: deactivateAccount() -> set isActive = false
    // TODO: Add method: verifyEmail() -> set isVerified = true
}

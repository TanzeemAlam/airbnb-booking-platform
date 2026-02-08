package com.airbnb.user.repository;

import com.airbnb.user.entity.User;
import com.airbnb.user.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository - Data access layer for User entity
 * 
 * Custom query methods:
 *   - Find user by email (unique)
 *   - Find active users
 *   - Find by role (HOST or GUEST)
 *   - Check if email exists
 * 
 * TODO: Implement all method bodies
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email
     * Used during login to retrieve user credentials
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if email already exists
     * Used during registration to prevent duplicate emails
     */
    boolean existsByEmail(String email);

    /**
     * Find active users with specific role
     * Used to get all hosts or guests
     */
    List<User> findByRoleAndIsActiveTrue(UserRole role);

    /**
     * Find all active users
     */
    List<User> findByIsActiveTrue();

    /**
     * Find verified users
     */
    List<User> findByIsVerifiedTrue();

    // TODO: Add more custom queries as needed:
    // TODO: - Find users by name (firstName or lastName)
    // TODO: - Find recent registrations
    // TODO: - Find users by creation date range
    // TODO: - Check if user with ID exists and is active
}

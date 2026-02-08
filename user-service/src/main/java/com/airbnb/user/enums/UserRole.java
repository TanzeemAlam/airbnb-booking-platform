package com.airbnb.user.enums;

/**
 * UserRole - Enum for user roles in the system
 * 
 * GUEST: Can search, view, and book listings
 * HOST: Can create, manage listings and accept bookings
 * ADMIN: System administrator with full access
 */
public enum UserRole {
    GUEST,      // Regular user who books properties
    HOST,       // Property owner who creates listings
    ADMIN       // System administrator
}

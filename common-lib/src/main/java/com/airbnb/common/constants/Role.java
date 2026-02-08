package com.airbnb.common.constants;

/**
 * User role constants
 */
public final class Role {
    public static final String ADMIN = "ADMIN";
    public static final String HOST = "HOST";
    public static final String GUEST = "GUEST";

    private Role() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}

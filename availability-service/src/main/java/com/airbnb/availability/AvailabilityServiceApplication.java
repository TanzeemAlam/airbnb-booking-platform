package com.airbnb.availability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Availability Service - Handles booking calendars and distributed locking with Redis.
 * Port: 8003
 * Database: PostgreSQL
 * Cache: Redis
 */
@SpringBootApplication
@EnableAsync
public class AvailabilityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AvailabilityServiceApplication.class, args);
    }
}

package com.airbnb.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Booking Service - Orchestrates booking flow with Saga pattern and Kafka events.
 * Port: 8004
 * Database: MySQL
 * Messaging: Kafka
 */
@SpringBootApplication
@EnableAsync
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}

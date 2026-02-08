package com.airbnb.listing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Listing Service - Handles property listing creation, viewing, and search.
 * Port: 8002
 * Database: PostgreSQL
 */
@SpringBootApplication
@EnableAsync
public class ListingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ListingServiceApplication.class, args);
    }
}

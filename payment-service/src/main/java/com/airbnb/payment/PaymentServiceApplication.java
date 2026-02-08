package com.airbnb.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Payment Service - Handles Stripe integration with resilience patterns (Circuit Breaker, Retry, Timeout).
 * Port: 8005
 * Database: MySQL
 * External: Stripe API
 */
@SpringBootApplication
@EnableAsync
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}

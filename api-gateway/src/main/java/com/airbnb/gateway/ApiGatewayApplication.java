package com.airbnb.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway - Routes requests to downstream services and provides Spring AI chatbot endpoint.
 * Port: 8000
 * Framework: Spring Cloud Gateway
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}

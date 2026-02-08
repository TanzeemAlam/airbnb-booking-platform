package com.airbnb.common.config;

import io.micrometer.tracing.Tracer;
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Observability configuration for distributed tracing
 * Auto-configures Spring Boot Actuator + Micrometer
 * W3C Trace Context standard for trace ID propagation
 */
@Configuration
@Import(MicrometerTracingAutoConfiguration.class)
public class ObservabilityConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ObservabilityConfiguration.class);

    public ObservabilityConfiguration() {
        logger.info("Observability configuration loaded - Micrometer tracing enabled with W3C Trace Context");
    }
}

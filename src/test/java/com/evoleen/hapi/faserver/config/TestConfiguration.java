package com.evoleen.hapi.faserver.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Test configuration class for Fire Arrow authentication tests.
 * Provides the necessary Spring configuration for test execution.
 */
@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties({TestConfigurationProperties.class})
@ComponentScan(basePackages = "com.evoleen.hapi.faserver")
public class TestConfiguration {
    // Configuration class for test beans
}
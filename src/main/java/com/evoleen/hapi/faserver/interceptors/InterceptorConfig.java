package com.evoleen.hapi.faserver.interceptors;

import com.evoleen.hapi.faserver.auth.AuthConfigurationProperties;
import com.evoleen.hapi.faserver.auth.AuthProviderManager;
import ca.uhn.fhir.rest.server.RestfulServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration class that registers FHIR security interceptors with the HAPI FHIR server.
 * 
 * This configuration ensures that authentication and authorization interceptors are
 * properly registered with the HAPI FHIR server when authentication is enabled.
 */
@Configuration
@ConditionalOnProperty(prefix = "hapi.fhir.auth", name = "enabled", havingValue = "true")
public class InterceptorConfig {

    private static final Logger logger = LoggerFactory.getLogger(InterceptorConfig.class);
    
    private final AuthConfigurationProperties authConfig;
    private final AuthProviderManager authProviderManager;
    
    @Autowired(required = false)
    private RestfulServer fhirServer;
    
    public InterceptorConfig(
            AuthConfigurationProperties authConfig,
            AuthProviderManager authProviderManager) {
        this.authConfig = authConfig;
        this.authProviderManager = authProviderManager;
    }
    
    /**
     * Registers the authentication and authorization interceptors with the FHIR server
     * after the Spring context is fully initialized.
     */
    @PostConstruct
    public void registerInterceptors() {
        if (fhirServer == null) {
            logger.warn("FHIR server not available for interceptor registration");
            return;
        }
        
        if (!authConfig.isEnabled()) {
            logger.info("Authentication is disabled, skipping interceptor registration");
            return;
        }
        
        try {
            // Create and register authentication interceptor
            AuthenticationInterceptor authenticationInterceptor = new AuthenticationInterceptor(
                    authProviderManager);
            
            fhirServer.registerInterceptor(authenticationInterceptor);
            logger.info("Registered JWT Authentication Interceptor");
            
            // Create and register authorization interceptor
            AuthorizationInterceptor authorizationInterceptor = new AuthorizationInterceptor(authConfig);
            
            fhirServer.registerInterceptor(authorizationInterceptor);
            logger.info("Registered JWT Authorization Interceptor");
            
            logger.info("Successfully registered FHIR security interceptors");
            
        } catch (Exception e) {
            logger.error("Failed to register FHIR security interceptors: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to register FHIR security interceptors", e);
        }
    }
}

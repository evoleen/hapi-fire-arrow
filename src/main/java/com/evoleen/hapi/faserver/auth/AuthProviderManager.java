package com.evoleen.hapi.faserver.auth;

import ca.uhn.fhir.jpa.starter.security.JwtValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Manages multiple authentication providers and cycles through them for token validation.
 * Implements the AuthProvider pattern where each provider attempts validation until one succeeds.
 */
@Service
@ConditionalOnProperty(prefix = "hapi.fhir.auth", name = "enabled", havingValue = "true")
public class AuthProviderManager {

    private static final Logger logger = LoggerFactory.getLogger(AuthProviderManager.class);

    private final List<AuthProvider> authProviders;
    private final AuthConfigurationProperties config;

    public AuthProviderManager(AuthProviderFactory factory, AuthConfigurationProperties config) {
        this.config = config;
        this.authProviders = factory.createProviders(config);
        logger.info("Initialized {} authentication providers", authProviders.size());
    }

    /**
     * Validates a JWT token using the configured authentication providers.
     * Cycles through providers until one successfully validates the token.
     *
     * @param token the JWT token to validate
     * @return validation result from the first provider that successfully validates
     */
    public JwtValidationResult validateToken(String token) {
        if (authProviders.isEmpty()) {
            logger.warn("No authentication providers configured");
            return JwtValidationResult.failure("No authentication providers available");
        }

        JwtValidationResult lastResult = null;
        
        for (AuthProvider provider : authProviders) {
            if (!provider.isEnabled()) {
                continue;
            }

            try {
                JwtValidationResult result = provider.validateToken(token);
                if (result.isValid()) {
                    logger.debug("Token validated successfully by provider: {}", provider.getName());
                    return result;
                }
                
                lastResult = result;
                logger.debug("Token validation failed for provider {}: {}", 
                           provider.getName(), result.getErrorMessage());
                           
            } catch (Exception e) {
                logger.warn("Provider {} threw exception during validation", provider.getName(), e);
                lastResult = JwtValidationResult.failure("Provider validation error: " + e.getMessage());
            }
        }

        // If we get here, all providers failed
        String errorMessage = lastResult != null ? lastResult.getErrorMessage() : "All providers failed";
        logger.warn("Token validation failed for all {} providers. Last error: {}", 
                   authProviders.size(), errorMessage);
        return JwtValidationResult.failure("Authentication failed: " + errorMessage);
    }

    /**
     * Returns whether authentication is required for all requests.
     *
     * @return true if authentication is required
     */
    public boolean isAuthenticationRequired() {
        return config.isRequired();
    }

    /**
     * Returns the list of configured authentication providers.
     *
     * @return list of auth providers
     */
    public List<AuthProvider> getProviders() {
        return authProviders;
    }
}

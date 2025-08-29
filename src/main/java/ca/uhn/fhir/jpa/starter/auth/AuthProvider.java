package ca.uhn.fhir.jpa.starter.auth;

import ca.uhn.fhir.jpa.starter.security.JwtValidationResult;

/**
 * Base interface for authentication providers.
 * Each concrete implementation validates tokens using different OAuth/OIDC mechanisms.
 */
public interface AuthProvider {

    /**
     * Validates the provided JWT token and returns validation result.
     * 
     * @param token the JWT token to validate
     * @return validation result containing user identity or error details
     */
    JwtValidationResult validateToken(String token);

    /**
     * Returns the provider type identifier.
     * 
     * @return provider type (e.g., "oauth", "azure_identity")
     */
    String getType();

    /**
     * Returns whether this provider is enabled.
     * 
     * @return true if provider is enabled
     */
    boolean isEnabled();

    /**
     * Returns the provider name/identifier.
     * 
     * @return provider name
     */
    String getName();
}

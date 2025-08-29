package com.evoleen.hapi.faserver.security;

import com.nimbusds.jwt.JWTClaimsSet;

/**
 * Result of JWT token validation containing the validation status, claims, and error messages.
 */
public class JwtValidationResult {
    
    private final boolean valid;
    private final JWTClaimsSet claimsSet;
    private final String errorMessage;
    private final UserIdentity userIdentity;
    private final String providerName;
    
    private JwtValidationResult(boolean valid, JWTClaimsSet claimsSet, String errorMessage, UserIdentity userIdentity, String providerName) {
        this.valid = valid;
        this.claimsSet = claimsSet;
        this.errorMessage = errorMessage;
        this.userIdentity = userIdentity;
        this.providerName = providerName;
    }
    
    /**
     * Creates a valid result with claims
     */
    public static JwtValidationResult valid(JWTClaimsSet claimsSet) {
        return new JwtValidationResult(true, claimsSet, null, null, null);
    }
    
    /**
     * Creates a valid result with user identity and provider
     */
    public static JwtValidationResult success(UserIdentity userIdentity, String providerName) {
        return new JwtValidationResult(true, null, null, userIdentity, providerName);
    }
    
    /**
     * Creates an invalid result with error message
     */
    public static JwtValidationResult invalid(String errorMessage) {
        return new JwtValidationResult(false, null, errorMessage, null, null);
    }
    
    /**
     * Creates an invalid result with error message
     */
    public static JwtValidationResult failure(String errorMessage) {
        return new JwtValidationResult(false, null, errorMessage, null, null);
    }
    
    /**
     * Returns whether the token is valid
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Returns the JWT claims set (null if invalid)
     */
    public JWTClaimsSet getClaimsSet() {
        return claimsSet;
    }
    
    /**
     * Returns the error message (null if valid)
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Returns the user identity (null if invalid)
     */
    public UserIdentity getUserIdentity() {
        return userIdentity;
    }
    
    /**
     * Returns the provider name (null if invalid)
     */
    public String getProviderName() {
        return providerName;
    }
    
    @Override
    public String toString() {
        return "JwtValidationResult{" +
                "valid=" + valid +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}

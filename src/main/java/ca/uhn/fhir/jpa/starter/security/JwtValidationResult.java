package ca.uhn.fhir.jpa.starter.security;

import com.nimbusds.jwt.JWTClaimsSet;

/**
 * Result of JWT token validation containing the validation status, claims, and error messages.
 */
public class JwtValidationResult {
    
    private final boolean valid;
    private final JWTClaimsSet claimsSet;
    private final String errorMessage;
    
    private JwtValidationResult(boolean valid, JWTClaimsSet claimsSet, String errorMessage) {
        this.valid = valid;
        this.claimsSet = claimsSet;
        this.errorMessage = errorMessage;
    }
    
    /**
     * Creates a valid result with claims
     */
    public static JwtValidationResult valid(JWTClaimsSet claimsSet) {
        return new JwtValidationResult(true, claimsSet, null);
    }
    
    /**
     * Creates an invalid result with error message
     */
    public static JwtValidationResult invalid(String errorMessage) {
        return new JwtValidationResult(false, null, errorMessage);
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
    
    @Override
    public String toString() {
        return "JwtValidationResult{" +
                "valid=" + valid +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}

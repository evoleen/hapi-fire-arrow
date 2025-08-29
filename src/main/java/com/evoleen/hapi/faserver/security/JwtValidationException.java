package com.evoleen.hapi.faserver.security;

/**
 * Exception thrown when JWT token validation fails.
 */
public class JwtValidationException extends Exception {
    
    public JwtValidationException(String message) {
        super(message);
    }
    
    public JwtValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

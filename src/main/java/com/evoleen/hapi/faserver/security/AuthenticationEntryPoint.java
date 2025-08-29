package com.evoleen.hapi.faserver.security;

/**
 * Marker interface for authentication entry point components.
 * This interface can be extended to provide additional authentication entry point capabilities.
 * 
 * The main implementation is provided by {@link CustomAuthenticationEntryPoint}.
 */
public interface AuthenticationEntryPoint extends org.springframework.security.web.AuthenticationEntryPoint {
    // This interface extends Spring Security's AuthenticationEntryPoint
    // to allow for custom authentication entry point implementations
}

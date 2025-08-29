package com.evoleen.hapi.faserver.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import com.evoleen.hapi.faserver.auth.AuthProviderManager;
import com.evoleen.hapi.faserver.security.JwtValidationResult;
import com.evoleen.hapi.faserver.security.UserIdentity;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * HAPI FHIR Authentication Interceptor that validates JWT tokens in incoming requests.
 * 
 * This interceptor hooks into the HAPI FHIR request processing pipeline to:
 * - Extract JWT tokens from Authorization headers
 * - Validate tokens using configured OAuth providers via AuthProviderManager
 * - Extract user identity information from token claims
 * - Store user identity in request context for authorization
 * 
 * Supports multiple OAuth providers including Azure Identity and standard OAuth/OIDC.
 */
@Component
@Interceptor
@ConditionalOnProperty(prefix = "hapi.fhir.auth", name = "enabled", havingValue = "true")
public class AuthenticationInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationInterceptor.class);
    
    public static final String USER_IDENTITY_ATTRIBUTE = "ca.uhn.fhir.jpa.starter.security.UserIdentity";
    public static final String AUTH_PROVIDER_ATTRIBUTE = "ca.uhn.fhir.jpa.starter.security.AuthProvider";
    
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    
    private final AuthProviderManager authProviderManager;
    
    public AuthenticationInterceptor(AuthProviderManager authProviderManager) {
        this.authProviderManager = authProviderManager;
    }
    
    /**
     * Intercepts incoming HTTP requests to validate JWT tokens.
     * 
     * This hook point is called early in the HAPI FHIR request processing pipeline,
     * allowing us to authenticate the user before any FHIR operations are performed.
     */
    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
    public boolean incomingRequestPreProcessed(RequestDetails theRequestDetails) {
        
        // Skip authentication for certain endpoints if configured
        if (shouldSkipAuthentication(theRequestDetails)) {
            logger.debug("Skipping authentication for endpoint: {}", theRequestDetails.getRequestPath());
            return true;
        }
        
        try {
            // Extract token from request
            String token = extractTokenFromRequest(theRequestDetails);
            
            if (!StringUtils.hasText(token)) {
                handleMissingToken(theRequestDetails);
                return false;
            }
            
            // Validate the JWT token using AuthProviderManager
            JwtValidationResult validationResult = authProviderManager.validateToken(token);
            
            if (!validationResult.isValid()) {
                logger.warn("JWT token validation failed: {}", validationResult.getErrorMessage());
                throw new AuthenticationException("Authentication failed: " + validationResult.getErrorMessage());
            }
            
            // Store user identity and provider info in request context
            theRequestDetails.setAttribute(USER_IDENTITY_ATTRIBUTE, validationResult.getUserIdentity());
            theRequestDetails.setAttribute(AUTH_PROVIDER_ATTRIBUTE, validationResult.getProviderName());
            
            logger.debug("Successfully authenticated user: {} with provider: {}", 
                    validationResult.getUserIdentity().getUserId(), validationResult.getProviderName());
            
            return true;
            
        } catch (AuthenticationException e) {
            // Re-throw authentication exceptions
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during authentication: {}", e.getMessage(), e);
            throw new AuthenticationException("Authentication failed: Internal server error");
        }
    }
    
    /**
     * Extracts JWT token from the Authorization header
     */
    private String extractTokenFromRequest(RequestDetails theRequestDetails) {
        // Get the HTTP servlet request
        if (!(theRequestDetails instanceof ServletRequestDetails)) {
            logger.warn("Request is not a ServletRequestDetails, cannot extract token");
            return null;
        }
        
        ServletRequestDetails servletRequest = (ServletRequestDetails) theRequestDetails;
        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        
        // Get Authorization header
        String authHeader = httpRequest.getHeader(AUTHORIZATION_HEADER);
        
        if (!StringUtils.hasText(authHeader)) {
            return null;
        }
        
        // Extract Bearer token
        if (authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }
        
        logger.debug("Authorization header does not contain Bearer token: {}", authHeader);
        return null;
    }
    
    /**
     * Determines if authentication should be skipped for this request
     */
    private boolean shouldSkipAuthentication(RequestDetails theRequestDetails) {
        String requestPath = theRequestDetails.getRequestPath();
        
        if (!StringUtils.hasText(requestPath)) {
            return false;
        }
        
        // Skip authentication for certain endpoints
        if (requestPath.equals("/metadata") || 
            requestPath.equals("/fhir/metadata") ||
            requestPath.startsWith("/actuator/") ||
            requestPath.startsWith("/swagger") ||
            requestPath.startsWith("/api-docs")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Handles requests with missing authentication tokens
     */
    private void handleMissingToken(RequestDetails theRequestDetails) {
        // Check if authentication is required for this endpoint
        if (authProviderManager.isAuthenticationRequired() && !shouldSkipAuthentication(theRequestDetails)) {
            logger.warn("Missing authentication token for protected endpoint: {}", 
                    theRequestDetails.getRequestPath());
            throw new AuthenticationException("Authentication required: Missing or invalid Authorization header");
        }
    }
    
    /**
     * Gets the authenticated user identity from the current request context
     */
    public static UserIdentity getCurrentUserIdentity(RequestDetails theRequestDetails) {
        if (theRequestDetails == null) {
            return null;
        }
        
        Object userIdentity = theRequestDetails.getAttribute(USER_IDENTITY_ATTRIBUTE);
        if (userIdentity instanceof UserIdentity) {
            return (UserIdentity) userIdentity;
        }
        
        return null;
    }
    
    /**
     * Gets the authentication provider used for the current request
     */
    public static String getCurrentAuthProvider(RequestDetails theRequestDetails) {
        if (theRequestDetails == null) {
            return null;
        }
        
        Object provider = theRequestDetails.getAttribute(AUTH_PROVIDER_ATTRIBUTE);
        if (provider instanceof String) {
            return (String) provider;
        }
        
        return null;
    }
}

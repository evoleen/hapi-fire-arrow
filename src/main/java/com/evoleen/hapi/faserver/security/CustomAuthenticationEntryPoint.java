package com.evoleen.hapi.faserver.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom authentication entry point for OAuth 2.0 JWT authentication.
 * Handles authentication failures and returns appropriate FHIR-compliant error responses.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationEntryPoint.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, 
                        AuthenticationException authException) throws IOException, ServletException {
        
        logger.warn("Authentication failed for request to {}: {}", 
            request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Add security headers
        response.setHeader("WWW-Authenticate", createWwwAuthenticateHeader(authException));
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // Create FHIR-compliant error response
        Map<String, Object> errorResponse = createFhirErrorResponse(request, authException);
        
        try {
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        } catch (IOException e) {
            logger.error("Error writing authentication error response", e);
        }
    }

    /**
     * Creates a WWW-Authenticate header based on the authentication exception.
     */
    private String createWwwAuthenticateHeader(AuthenticationException authException) {
        StringBuilder header = new StringBuilder("Bearer");
        
        if (authException instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException oauth2Exception = (OAuth2AuthenticationException) authException;
            org.springframework.security.oauth2.core.OAuth2Error error = oauth2Exception.getError();
            
            if (error != null) {
                header.append(" error=\"")
                      .append(error.getErrorCode())
                      .append("\"");
                
                if (error.getDescription() != null) {
                    header.append(", error_description=\"")
                          .append(error.getDescription())
                          .append("\"");
                }
                
                if (error.getUri() != null) {
                    header.append(", error_uri=\"")
                          .append(error.getUri())
                          .append("\"");
                }
            }
        } else {
            header.append(" error=\"invalid_token\"");
            header.append(", error_description=\"")
                  .append(sanitizeErrorMessage(authException.getMessage()))
                  .append("\"");
        }
        
        return header.toString();
    }

    /**
     * Creates a FHIR-compliant OperationOutcome error response.
     */
    private Map<String, Object> createFhirErrorResponse(HttpServletRequest request, AuthenticationException authException) {
        Map<String, Object> operationOutcome = new HashMap<>();
        operationOutcome.put("resourceType", "OperationOutcome");
        operationOutcome.put("id", "auth-error-" + System.currentTimeMillis());
        
        // Meta information
        Map<String, Object> meta = new HashMap<>();
        meta.put("lastUpdated", Instant.now().toString());
        operationOutcome.put("meta", meta);
        
        // Issue details
        Map<String, Object> issue = new HashMap<>();
        issue.put("severity", "error");
        issue.put("code", "security");
        
        // Determine the specific error details
        String errorDetails = determineErrorDetails(authException);
        issue.put("details", Map.of(
            "coding", java.util.Arrays.asList(Map.of(
                "system", "http://terminology.hl7.org/CodeSystem/operation-outcome",
                "code", "security",
                "display", "Security Problem"
            )),
            "text", errorDetails
        ));
        
        issue.put("diagnostics", "Authentication required for access to FHIR resources. " + 
            "Please provide a valid Bearer token in the Authorization header.");
        
        // Location information
        issue.put("location", java.util.Arrays.asList(request.getRequestURI()));
        
        operationOutcome.put("issue", java.util.Arrays.asList(issue));
        
        return operationOutcome;
    }

    /**
     * Determines appropriate error details based on the authentication exception type.
     */
    private String determineErrorDetails(AuthenticationException authException) {
        if (authException instanceof OAuth2AuthenticationException) {
            OAuth2AuthenticationException oauth2Exception = (OAuth2AuthenticationException) authException;
            org.springframework.security.oauth2.core.OAuth2Error error = oauth2Exception.getError();
            
            if (error != null) {
                String errorCode = error.getErrorCode();
                
                switch (errorCode) {
                    case "invalid_token":
                        return "The provided access token is invalid, expired, or malformed";
                    case "insufficient_scope":
                        return "The access token does not have sufficient scope for this resource";
                    case "invalid_request":
                        return "The authentication request is malformed or missing required parameters";
                    default:
                        return "OAuth 2.0 authentication error: " + errorCode;
                }
            }
        }
        
        // Handle specific authentication exception messages
        String message = authException.getMessage();
        if (message != null) {
            if (message.contains("JWT")) {
                return "JWT token validation failed";
            } else if (message.contains("expired")) {
                return "Authentication token has expired";
            } else if (message.contains("signature")) {
                return "JWT token signature verification failed";
            } else if (message.contains("audience")) {
                return "JWT token audience validation failed";
            } else if (message.contains("issuer")) {
                return "JWT token issuer validation failed";
            }
        }
        
        return "Authentication credentials are missing or invalid";
    }

    /**
     * Sanitizes error messages to prevent information leakage.
     */
    private String sanitizeErrorMessage(String message) {
        if (message == null) {
            return "Authentication failed";
        }
        
        // Remove potentially sensitive information from error messages
        message = message.replaceAll("Bearer [A-Za-z0-9._-]+", "Bearer [REDACTED]");
        message = message.replaceAll("token=[A-Za-z0-9._-]+", "token=[REDACTED]");
        
        // Limit message length to prevent verbose error disclosure
        if (message.length() > 200) {
            message = message.substring(0, 200) + "...";
        }
        
        return message;
    }
}

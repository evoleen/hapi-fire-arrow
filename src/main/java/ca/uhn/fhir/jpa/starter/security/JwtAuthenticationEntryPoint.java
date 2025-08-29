package ca.uhn.fhir.jpa.starter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom authentication entry point for JWT authentication errors.
 * Provides proper HTTP responses and error messages for invalid/expired tokens.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        
        logger.warn("JWT authentication failed for request {}: {}", 
                request.getRequestURI(), authException.getMessage());
        
        // Set response status and content type
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        // Create error response body
        Map<String, Object> errorResponse = createErrorResponse(request, authException);
        
        // Write error response
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
    
    private Map<String, Object> createErrorResponse(HttpServletRequest request, 
                                                  AuthenticationException authException) {
        Map<String, Object> errorResponse = new HashMap<>();
        
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", getErrorMessage(authException));
        errorResponse.put("path", request.getRequestURI());
        
        // Add FHIR-specific error details if this is a FHIR request
        if (isFhirRequest(request)) {
            errorResponse.put("resourceType", "OperationOutcome");
            errorResponse.put("issue", createFhirIssue(authException));
        }
        
        return errorResponse;
    }
    
    private String getErrorMessage(AuthenticationException authException) {
        String message = authException.getMessage();
        
        // Provide more user-friendly error messages
        if (message != null) {
            if (message.contains("expired")) {
                return "JWT token has expired. Please obtain a new token and retry the request.";
            } else if (message.contains("invalid")) {
                return "JWT token is invalid. Please check the token format and signature.";
            } else if (message.contains("audience")) {
                return "JWT token audience is invalid for this service.";
            } else if (message.contains("issuer")) {
                return "JWT token issuer is not trusted by this service.";
            } else if (message.contains("signature")) {
                return "JWT token signature verification failed.";
            }
        }
        
        return "Authentication failed. Please provide a valid JWT token.";
    }
    
    private boolean isFhirRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith("/fhir");
    }
    
    private Map<String, Object> createFhirIssue(AuthenticationException authException) {
        Map<String, Object> issue = new HashMap<>();
        issue.put("severity", "error");
        issue.put("code", "security");
        
        Map<String, Object> details = new HashMap<>();
        details.put("text", getErrorMessage(authException));
        issue.put("details", details);
        
        return issue;
    }
}

package com.evoleen.hapi.faserver.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom access denied handler for JWT authorization errors.
 * Handles cases where authentication succeeds but authorization fails.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(JwtAccessDeniedHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        logger.warn("Access denied for authenticated user on request {}: {}", 
                request.getRequestURI(), accessDeniedException.getMessage());
        
        // Set response status and content type
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        // Create error response body
        Map<String, Object> errorResponse = createErrorResponse(request, accessDeniedException);
        
        // Write error response
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
    
    private Map<String, Object> createErrorResponse(HttpServletRequest request, 
                                                  AccessDeniedException accessDeniedException) {
        Map<String, Object> errorResponse = new HashMap<>();
        
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", HttpServletResponse.SC_FORBIDDEN);
        errorResponse.put("error", "Forbidden");
        errorResponse.put("message", "Access denied. You do not have permission to access this resource.");
        errorResponse.put("path", request.getRequestURI());
        
        // Add FHIR-specific error details if this is a FHIR request
        if (isFhirRequest(request)) {
            errorResponse.put("resourceType", "OperationOutcome");
            errorResponse.put("issue", createFhirIssue());
        }
        
        return errorResponse;
    }
    
    private boolean isFhirRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith("/fhir");
    }
    
    private Map<String, Object> createFhirIssue() {
        Map<String, Object> issue = new HashMap<>();
        issue.put("severity", "error");
        issue.put("code", "forbidden");
        
        Map<String, Object> details = new HashMap<>();
        details.put("text", "Access denied. You do not have permission to access this resource.");
        issue.put("details", details);
        
        return issue;
    }
}

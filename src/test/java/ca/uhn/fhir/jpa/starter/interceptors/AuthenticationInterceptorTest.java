package ca.uhn.fhir.jpa.starter.interceptors;

import ca.uhn.fhir.jpa.starter.config.AuthConfigurationProperties;
import ca.uhn.fhir.jpa.starter.security.*;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthenticationInterceptor
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationInterceptorTest {

    @Mock
    private JwtTokenValidator jwtTokenValidator;
    
    @Mock
    private TokenClaimExtractor tokenClaimExtractor;
    
    @Mock
    private AuthConfigurationProperties authConfig;
    
    @Mock
    private ServletRequestDetails requestDetails;
    
    @Mock
    private HttpServletRequest httpRequest;
    
    private AuthenticationInterceptor interceptor;
    
    @BeforeEach
    void setUp() {
        interceptor = new AuthenticationInterceptor(jwtTokenValidator, tokenClaimExtractor, authConfig);
        
        // Default config setup
        when(authConfig.isEnabled()).thenReturn(true);
        when(authConfig.isRequired()).thenReturn(true);
        when(requestDetails.getServletRequest()).thenReturn(httpRequest);
    }
    
    @Test
    void testSuccessfulAuthentication() throws Exception {
        // Setup
        String token = "valid.jwt.token";
        String providerName = "test-provider";
        
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(requestDetails.getRequestPath()).thenReturn("/fhir/Patient");
        
        // Mock provider configuration
        Map<String, AuthConfigurationProperties.OAuthProvider> providers = new HashMap<>();
        AuthConfigurationProperties.OAuthProvider provider = mock(AuthConfigurationProperties.OAuthProvider.class);
        when(provider.isEnabled()).thenReturn(true);
        providers.put(providerName, provider);
        when(authConfig.getProviders()).thenReturn(providers);
        
        // Mock JWT validation
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("user123")
                .claim("roles", Arrays.asList("practitioner"))
                .build();
        JwtValidationResult validationResult = JwtValidationResult.valid(claimsSet);
        when(jwtTokenValidator.validateToken(token, providerName)).thenReturn(validationResult);
        
        // Mock user identity extraction
        UserIdentity userIdentity = new UserIdentity.Builder()
                .userId("user123")
                .roles(Set.of("practitioner"))
                .fhirUserRoleResourceType("Practitioner")
                .build();
        when(tokenClaimExtractor.extractUserIdentity(claimsSet, providerName)).thenReturn(userIdentity);
        when(tokenClaimExtractor.validateFhirClaims(userIdentity)).thenReturn(true);
        
        // Execute
        boolean result = interceptor.incomingRequestPreProcessed(requestDetails);
        
        // Verify
        assertTrue(result);
        verify(requestDetails).setAttribute(AuthenticationInterceptor.USER_IDENTITY_ATTRIBUTE, userIdentity);
        verify(requestDetails).setAttribute(AuthenticationInterceptor.AUTH_PROVIDER_ATTRIBUTE, providerName);
    }
    
    @Test
    void testMissingAuthorizationHeader() {
        // Setup
        when(httpRequest.getHeader("Authorization")).thenReturn(null);
        when(requestDetails.getRequestPath()).thenReturn("/fhir/Patient");
        
        // Execute & Verify
        assertThrows(AuthenticationException.class, () -> {
            interceptor.incomingRequestPreProcessed(requestDetails);
        });
    }
    
    @Test
    void testInvalidTokenFormat() {
        // Setup
        when(httpRequest.getHeader("Authorization")).thenReturn("Invalid token format");
        when(requestDetails.getRequestPath()).thenReturn("/fhir/Patient");
        
        // Execute & Verify
        assertThrows(AuthenticationException.class, () -> {
            interceptor.incomingRequestPreProcessed(requestDetails);
        });
    }
    
    @Test
    void testJwtValidationFailure() throws Exception {
        // Setup
        String token = "invalid.jwt.token";
        String providerName = "test-provider";
        
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(requestDetails.getRequestPath()).thenReturn("/fhir/Patient");
        
        // Mock provider configuration
        Map<String, AuthConfigurationProperties.OAuthProvider> providers = new HashMap<>();
        AuthConfigurationProperties.OAuthProvider provider = mock(AuthConfigurationProperties.OAuthProvider.class);
        when(provider.isEnabled()).thenReturn(true);
        providers.put(providerName, provider);
        when(authConfig.getProviders()).thenReturn(providers);
        
        // Mock JWT validation failure
        JwtValidationResult validationResult = JwtValidationResult.invalid("Token expired");
        when(jwtTokenValidator.validateToken(token, providerName)).thenReturn(validationResult);
        
        // Execute & Verify
        assertThrows(AuthenticationException.class, () -> {
            interceptor.incomingRequestPreProcessed(requestDetails);
        });
    }
    
    @Test
    void testSkipAuthenticationForMetadataEndpoint() throws Exception {
        // Setup
        when(requestDetails.getRequestPath()).thenReturn("/metadata");
        
        // Execute
        boolean result = interceptor.incomingRequestPreProcessed(requestDetails);
        
        // Verify
        assertTrue(result);
        verify(jwtTokenValidator, never()).validateToken(anyString(), anyString());
    }
    
    @Test
    void testSkipAuthenticationWhenDisabled() throws Exception {
        // Setup
        when(authConfig.isEnabled()).thenReturn(false);
        when(requestDetails.getRequestPath()).thenReturn("/fhir/Patient");
        
        // Execute
        boolean result = interceptor.incomingRequestPreProcessed(requestDetails);
        
        // Verify
        assertTrue(result);
        verify(jwtTokenValidator, never()).validateToken(anyString(), anyString());
    }
    
    @Test
    void testUserIdentityExtractionFailure() throws Exception {
        // Setup
        String token = "valid.jwt.token";
        String providerName = "test-provider";
        
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(requestDetails.getRequestPath()).thenReturn("/fhir/Patient");
        
        // Mock provider configuration
        Map<String, AuthConfigurationProperties.OAuthProvider> providers = new HashMap<>();
        AuthConfigurationProperties.OAuthProvider provider = mock(AuthConfigurationProperties.OAuthProvider.class);
        when(provider.isEnabled()).thenReturn(true);
        providers.put(providerName, provider);
        when(authConfig.getProviders()).thenReturn(providers);
        
        // Mock JWT validation success but identity extraction failure
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("user123").build();
        JwtValidationResult validationResult = JwtValidationResult.valid(claimsSet);
        when(jwtTokenValidator.validateToken(token, providerName)).thenReturn(validationResult);
        when(tokenClaimExtractor.extractUserIdentity(claimsSet, providerName)).thenReturn(null);
        
        // Execute & Verify
        assertThrows(AuthenticationException.class, () -> {
            interceptor.incomingRequestPreProcessed(requestDetails);
        });
    }
    
    @Test
    void testGetCurrentUserIdentity() {
        // Setup
        UserIdentity userIdentity = new UserIdentity.Builder()
                .userId("user123")
                .build();
        when(requestDetails.getAttribute(AuthenticationInterceptor.USER_IDENTITY_ATTRIBUTE))
                .thenReturn(userIdentity);
        
        // Execute
        UserIdentity result = AuthenticationInterceptor.getCurrentUserIdentity(requestDetails);
        
        // Verify
        assertEquals(userIdentity, result);
    }
    
    @Test
    void testGetCurrentUserIdentityWithNullRequest() {
        // Execute
        UserIdentity result = AuthenticationInterceptor.getCurrentUserIdentity(null);
        
        // Verify
        assertNull(result);
    }
    
    @Test
    void testGetCurrentAuthProvider() {
        // Setup
        String providerName = "test-provider";
        when(requestDetails.getAttribute(AuthenticationInterceptor.AUTH_PROVIDER_ATTRIBUTE))
                .thenReturn(providerName);
        
        // Execute
        String result = AuthenticationInterceptor.getCurrentAuthProvider(requestDetails);
        
        // Verify
        assertEquals(providerName, result);
    }
    
    // Additional comprehensive test scenarios
    
    @Test
    void testMultipleProvidersAuthentication() throws Exception {
        // Test authentication with multiple OAuth providers
        String token = "valid.jwt.token";
        
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(requestDetails.getRequestPath()).thenReturn("/fhir/Patient");
        
        // Mock multiple providers
        Map<String, AuthConfigurationProperties.OAuthProvider> providers = new HashMap<>();
        
        // Standard provider
        AuthConfigurationProperties.OAuthProvider standardProvider = mock(AuthConfigurationProperties.OAuthProvider.class);
        when(standardProvider.isEnabled()).thenReturn(true);
        providers.put("standard-provider", standardProvider);
        
        // Azure provider
        AuthConfigurationProperties.OAuthProvider azureProvider = mock(AuthConfigurationProperties.OAuthProvider.class);
        when(azureProvider.isEnabled()).thenReturn(true);
        providers.put("azure-provider", azureProvider);
        
        when(authConfig.getProviders()).thenReturn(providers);
        
        // Mock successful validation for first provider
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("user123")
                .claim("roles", Arrays.asList("practitioner"))
                .build();
        JwtValidationResult validationResult = JwtValidationResult.valid(claimsSet);
        when(jwtTokenValidator.validateToken(token, "standard-provider")).thenReturn(validationResult);
        
        // Mock user identity extraction
        UserIdentity userIdentity = new UserIdentity.Builder()
                .userId("user123")
                .roles(Set.of("practitioner"))
                .build();
        when(tokenClaimExtractor.extractUserIdentity(claimsSet, "standard-provider")).thenReturn(userIdentity);
        when(tokenClaimExtractor.validateFhirClaims(userIdentity)).thenReturn(true);
        
        // Execute
        boolean result = interceptor.incomingRequestPreProcessed(requestDetails);
        
        // Verify
        assertTrue(result);
        verify(requestDetails).setAttribute(AuthenticationInterceptor.USER_IDENTITY_ATTRIBUTE, userIdentity);
        verify(requestDetails).setAttribute(AuthenticationInterceptor.AUTH_PROVIDER_ATTRIBUTE, "standard-provider");
    }
    
    @Test
    void testTokenRefreshScenario() throws Exception {
        // Test handling of token refresh scenarios
        String oldToken = "expired.jwt.token";
        String newToken = "refreshed.jwt.token";
        
        when(httpRequest.getHeader("Authorization"))
                .thenReturn("Bearer " + oldToken)
                .thenReturn("Bearer " + newToken);
        when(requestDetails.getRequestPath()).thenReturn("/fhir/Patient");
        
        Map<String, AuthConfigurationProperties.OAuthProvider> providers = new HashMap<>();
        AuthConfigurationProperties.OAuthProvider provider = mock(AuthConfigurationProperties.OAuthProvider.class);
        when(provider.isEnabled()).thenReturn(true);
        providers.put("test-provider", provider);
        when(authConfig.getProviders()).thenReturn(providers);
        
        // First request with expired token fails
        JwtValidationResult expiredResult = JwtValidationResult.invalid("Token expired");
        when(jwtTokenValidator.validateToken(oldToken, "test-provider")).thenReturn(expiredResult);
        
        // Second request with new token succeeds
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("user123")
                .build();
        JwtValidationResult validResult = JwtValidationResult.valid(claimsSet);
        when(jwtTokenValidator.validateToken(newToken, "test-provider")).thenReturn(validResult);
        
        UserIdentity userIdentity = new UserIdentity.Builder().userId("user123").build();
        when(tokenClaimExtractor.extractUserIdentity(claimsSet, "test-provider")).thenReturn(userIdentity);
        when(tokenClaimExtractor.validateFhirClaims(userIdentity)).thenReturn(true);
        
        // First request should fail
        assertThrows(AuthenticationException.class, () -> {
            interceptor.incomingRequestPreProcessed(requestDetails);
        });
        
        // Update header for second request
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer " + newToken);
        
        // Second request should succeed
        boolean result = interceptor.incomingRequestPreProcessed(requestDetails);
        assertTrue(result);
    }
    
    @Test
    void testConcurrentAuthentication() throws Exception {
        // Test thread safety with concurrent authentication requests
        String token = "valid.jwt.token";
        
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(requestDetails.getRequestPath()).thenReturn("/fhir/Patient");
        
        Map<String, AuthConfigurationProperties.OAuthProvider> providers = new HashMap<>();
        AuthConfigurationProperties.OAuthProvider provider = mock(AuthConfigurationProperties.OAuthProvider.class);
        when(provider.isEnabled()).thenReturn(true);
        providers.put("test-provider", provider);
        when(authConfig.getProviders()).thenReturn(providers);
        
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("user123")
                .build();
        JwtValidationResult validationResult = JwtValidationResult.valid(claimsSet);
        when(jwtTokenValidator.validateToken(token, "test-provider")).thenReturn(validationResult);
        
        UserIdentity userIdentity = new UserIdentity.Builder().userId("user123").build();
        when(tokenClaimExtractor.extractUserIdentity(claimsSet, "test-provider")).thenReturn(userIdentity);
        when(tokenClaimExtractor.validateFhirClaims(userIdentity)).thenReturn(true);
        
        // Execute multiple concurrent requests
        boolean result = interceptor.incomingRequestPreProcessed(requestDetails);
        assertTrue(result);
        
        // Verify thread-safe behavior
        verify(requestDetails).setAttribute(AuthenticationInterceptor.USER_IDENTITY_ATTRIBUTE, userIdentity);
        verify(requestDetails).setAttribute(AuthenticationInterceptor.AUTH_PROVIDER_ATTRIBUTE, "test-provider");
    }
    
    @Test
    void testInvalidTokenFormats() throws Exception {
        // Test various invalid token formats
        String[] invalidTokenFormats = {
            null,
            "",
            "Bearer",
            "Bearer ",
            "Basic dGVzdDp0ZXN0", // Wrong auth type
            "Bearertoken", // Missing space
            "Bearer token-without-dots",
            "Bearer too.few.parts",
            "Bearer too.many.parts.here.invalid"
        };
        
        when(requestDetails.getRequestPath()).thenReturn("/fhir/Patient");
        
        for (String invalidToken : invalidTokenFormats) {
            when(httpRequest.getHeader("Authorization")).thenReturn(invalidToken);
            
            assertThrows(AuthenticationException.class, () -> {
                interceptor.incomingRequestPreProcessed(requestDetails);
            }, "Should reject invalid token format: " + invalidToken);
        }
    }
    
    @Test
    void testWhitelistedEndpoints() throws Exception {
        // Test that certain endpoints bypass authentication
        String[] publicEndpoints = {
            "/metadata",
            "/fhir/metadata",
            "/actuator/health",
            "/actuator/info",
            "/.well-known/smart-configuration"
        };
        
        for (String endpoint : publicEndpoints) {
            when(requestDetails.getRequestPath()).thenReturn(endpoint);
            
            boolean result = interceptor.incomingRequestPreProcessed(requestDetails);
            
            assertTrue(result, "Public endpoint should bypass authentication: " + endpoint);
            verify(jwtTokenValidator, never()).validateToken(anyString(), anyString());
        }
    }
    
    @Test
    void testErrorHandling() throws Exception {
        // Test various error scenarios
        String token = "valid.jwt.token";
        
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(requestDetails.getRequestPath()).thenReturn("/fhir/Patient");
        
        Map<String, AuthConfigurationProperties.OAuthProvider> providers = new HashMap<>();
        AuthConfigurationProperties.OAuthProvider provider = mock(AuthConfigurationProperties.OAuthProvider.class);
        when(provider.isEnabled()).thenReturn(true);
        providers.put("test-provider", provider);
        when(authConfig.getProviders()).thenReturn(providers);
        
        // Test JwtValidationException
        when(jwtTokenValidator.validateToken(token, "test-provider"))
                .thenThrow(new JwtValidationException("Validation failed"));
        
        assertThrows(AuthenticationException.class, () -> {
            interceptor.incomingRequestPreProcessed(requestDetails);
        });
    }
    
    @Test
    void testFhirClaimValidationFailure() throws Exception {
        // Test FHIR claim validation failure
        String token = "valid.jwt.token";
        
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(requestDetails.getRequestPath()).thenReturn("/fhir/Patient");
        
        Map<String, AuthConfigurationProperties.OAuthProvider> providers = new HashMap<>();
        AuthConfigurationProperties.OAuthProvider provider = mock(AuthConfigurationProperties.OAuthProvider.class);
        when(provider.isEnabled()).thenReturn(true);
        providers.put("test-provider", provider);
        when(authConfig.getProviders()).thenReturn(providers);
        
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("user123")
                .build();
        JwtValidationResult validationResult = JwtValidationResult.valid(claimsSet);
        when(jwtTokenValidator.validateToken(token, "test-provider")).thenReturn(validationResult);
        
        UserIdentity userIdentity = new UserIdentity.Builder().userId("user123").build();
        when(tokenClaimExtractor.extractUserIdentity(claimsSet, "test-provider")).thenReturn(userIdentity);
        when(tokenClaimExtractor.validateFhirClaims(userIdentity)).thenReturn(false); // Validation fails
        
        // Execute & Verify
        assertThrows(AuthenticationException.class, () -> {
            interceptor.incomingRequestPreProcessed(requestDetails);
        });
    }
    
    @Test
    void testOptionalAuthenticationMode() throws Exception {
        // Test when authentication is not required
        when(authConfig.isEnabled()).thenReturn(true);
        when(authConfig.isRequired()).thenReturn(false); // Not required
        when(requestDetails.getRequestPath()).thenReturn("/fhir/Patient");
        when(httpRequest.getHeader("Authorization")).thenReturn(null); // No token
        
        // Execute
        boolean result = interceptor.incomingRequestPreProcessed(requestDetails);
        
        // Verify - should proceed without authentication
        assertTrue(result);
        verify(jwtTokenValidator, never()).validateToken(anyString(), anyString());
    }
    
    @Test
    void testDifferentHttpMethods() throws Exception {
        // Test authentication works for different HTTP methods
        String[] httpMethods = {"GET", "POST", "PUT", "DELETE", "PATCH"};
        String token = "valid.jwt.token";
        
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer " + token);
        
        Map<String, AuthConfigurationProperties.OAuthProvider> providers = new HashMap<>();
        AuthConfigurationProperties.OAuthProvider provider = mock(AuthConfigurationProperties.OAuthProvider.class);
        when(provider.isEnabled()).thenReturn(true);
        providers.put("test-provider", provider);
        when(authConfig.getProviders()).thenReturn(providers);
        
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("user123").build();
        JwtValidationResult validationResult = JwtValidationResult.valid(claimsSet);
        when(jwtTokenValidator.validateToken(token, "test-provider")).thenReturn(validationResult);
        
        UserIdentity userIdentity = new UserIdentity.Builder().userId("user123").build();
        when(tokenClaimExtractor.extractUserIdentity(claimsSet, "test-provider")).thenReturn(userIdentity);
        when(tokenClaimExtractor.validateFhirClaims(userIdentity)).thenReturn(true);
        
        for (String method : httpMethods) {
            when(requestDetails.getRequestPath()).thenReturn("/fhir/Patient");
            when(httpRequest.getMethod()).thenReturn(method);
            
            boolean result = interceptor.incomingRequestPreProcessed(requestDetails);
            assertTrue(result, "Authentication should work for HTTP method: " + method);
        }
    }
    
    @Test
    void testAzureSpecificAuthentication() throws Exception {
        // Test Azure-specific authentication flow
        String token = "azure.jwt.token";
        
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(requestDetails.getRequestPath()).thenReturn("/fhir/Patient");
        
        Map<String, AuthConfigurationProperties.OAuthProvider> providers = new HashMap<>();
        AuthConfigurationProperties.OAuthProvider azureProvider = mock(AuthConfigurationProperties.OAuthProvider.class);
        when(azureProvider.isEnabled()).thenReturn(true);
        providers.put("azure-provider", azureProvider);
        when(authConfig.getProviders()).thenReturn(providers);
        
        // Mock Azure-specific claims
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("azure-user-123")
                .claim("tid", "tenant-id")
                .claim("appid", "app-id")
                .claim("resource_type", "Practitioner")
                .build();
        JwtValidationResult validationResult = JwtValidationResult.valid(claimsSet);
        when(jwtTokenValidator.validateToken(token, "azure-provider")).thenReturn(validationResult);
        
        UserIdentity userIdentity = new UserIdentity.Builder()
                .userId("azure-user-123")
                .fhirUserRoleResourceType("Practitioner")
                .build();
        when(tokenClaimExtractor.extractUserIdentity(claimsSet, "azure-provider")).thenReturn(userIdentity);
        when(tokenClaimExtractor.validateFhirClaims(userIdentity)).thenReturn(true);
        
        // Execute
        boolean result = interceptor.incomingRequestPreProcessed(requestDetails);
        
        // Verify
        assertTrue(result);
        verify(requestDetails).setAttribute(AuthenticationInterceptor.USER_IDENTITY_ATTRIBUTE, userIdentity);
        verify(requestDetails).setAttribute(AuthenticationInterceptor.AUTH_PROVIDER_ATTRIBUTE, "azure-provider");
    }
    
    @Test
    void testPerformanceWithCachedTokens() throws Exception {
        // Test performance with cached token validation
        String token = "cached.jwt.token";
        String providerName = "test-provider";
        
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(requestDetails.getRequestPath()).thenReturn("/fhir/Patient");
        
        Map<String, AuthConfigurationProperties.OAuthProvider> providers = new HashMap<>();
        AuthConfigurationProperties.OAuthProvider provider = mock(AuthConfigurationProperties.OAuthProvider.class);
        when(provider.isEnabled()).thenReturn(true);
        providers.put(providerName, provider);
        when(authConfig.getProviders()).thenReturn(providers);
        
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("user123").build();
        JwtValidationResult validationResult = JwtValidationResult.valid(claimsSet);
        when(jwtTokenValidator.validateToken(token, providerName)).thenReturn(validationResult);
        
        UserIdentity userIdentity = new UserIdentity.Builder().userId("user123").build();
        when(tokenClaimExtractor.extractUserIdentity(claimsSet, providerName)).thenReturn(userIdentity);
        when(tokenClaimExtractor.validateFhirClaims(userIdentity)).thenReturn(true);
        
        // Measure performance over multiple calls
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            boolean result = interceptor.incomingRequestPreProcessed(requestDetails);
            assertTrue(result);
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // Should be fast with caching (less than 1 second for 100 calls)
        assertTrue(totalTime < 1000, "100 cached authentications should complete in under 1 second");
    }
}

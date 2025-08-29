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
    void testSkipAuthenticationForMetadataEndpoint() {
        // Setup
        when(requestDetails.getRequestPath()).thenReturn("/metadata");
        
        // Execute
        boolean result = interceptor.incomingRequestPreProcessed(requestDetails);
        
        // Verify
        assertTrue(result);
        verify(jwtTokenValidator, never()).validateToken(anyString(), anyString());
    }
    
    @Test
    void testSkipAuthenticationWhenDisabled() {
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
}

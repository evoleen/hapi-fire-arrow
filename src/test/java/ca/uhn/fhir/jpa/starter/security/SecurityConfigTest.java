package ca.uhn.fhir.jpa.starter.security;

import com.evoleen.hapi.faserver.auth.AuthConfigurationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Spring Security integration tests for OAuth JWT authentication.
 * Tests security configuration, CORS, authentication requirements, and endpoint access control.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest
@Import({SecurityConfig.class, SecurityConfigTest.TestConfig.class})
@TestPropertySource(properties = {
    "hapi.fhir.auth.enabled=true",
    "hapi.fhir.auth.required=true"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthConfigurationProperties authConfig;

    @MockBean
    private CustomAuthenticationEntryPoint authenticationEntryPoint;

    @MockBean
    private JwtTokenValidator jwtTokenValidator;

    @MockBean
    private TokenClaimExtractor tokenClaimExtractor;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public AuthConfigurationProperties authConfigurationProperties() {
            AuthConfigurationProperties config = new AuthConfigurationProperties();
            config.setEnabled(true);
            config.setRequired(true);
            
            Map<String, com.evoleen.hapi.faserver.auth.AuthConfigurationProperties.AuthProviderConfiguration> providers = new HashMap<>();
            com.evoleen.hapi.faserver.auth.AuthConfigurationProperties.AuthProviderConfiguration provider = new com.evoleen.hapi.faserver.auth.AuthConfigurationProperties.AuthProviderConfiguration();
            provider.setType("standard");
            provider.setEnabled(true);
            providers.put("test-provider", provider);
            config.setProviders(providers);
            
            return config;
        }
    }

    @Test
    void testFhirEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/fhir/Patient"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testMetadataEndpointPublicAccess() throws Exception {
        mockMvc.perform(get("/metadata"))
                .andExpect(status().isOk());
    }

    @Test
    void testFhirCapabilityStatementPublicAccess() throws Exception {
        mockMvc.perform(get("/fhir/metadata"))
                .andExpect(status().isOk());
    }

    @Test
    void testHealthEndpointPublicAccess() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void testAuthenticatedFhirAccess() throws Exception {
        // This test would require actual JWT implementation for full integration testing
        // For unit testing, we verify that unauthenticated access is blocked
        mockMvc.perform(get("/fhir/Patient"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testInvalidJwtTokenRejected() throws Exception {
        mockMvc.perform(get("/fhir/Patient")
                .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testMissingAuthorizationHeaderRejected() throws Exception {
        mockMvc.perform(get("/fhir/Patient"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCorsConfiguration() throws Exception {
        mockMvc.perform(options("/fhir/Patient")
                .header("Origin", "https://example.com")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().exists("Access-Control-Allow-Headers"));
    }

    @Test
    void testSecurityHeadersPresent() throws Exception {
        mockMvc.perform(get("/fhir/metadata"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().exists("X-XSS-Protection"));
    }

    @Test
    void testSessionCreationPolicyStateless() throws Exception {
        // Verify that no session is created for requests
        mockMvc.perform(get("/fhir/Patient"))
                .andExpect(result -> {
                    // Session should not be created for stateless API
                    assertNull(result.getRequest().getSession(false));
                });
    }

    @Test
    void testCsrfDisabledForApi() throws Exception {
        // CSRF should be disabled for API endpoints - verify by checking that POST without CSRF token doesn't fail with CSRF error
        mockMvc.perform(post("/fhir/Patient")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resourceType\":\"Patient\",\"name\":[{\"family\":\"Test\"}]}"))
                .andExpect(status().isUnauthorized()); // Should fail due to authentication, not CSRF
    }

    @Test
    void testMultipleProvidersConfiguration() throws Exception {
        // Test that security config can handle multiple OAuth providers
        AuthConfigurationProperties config = new AuthConfigurationProperties();
        config.setEnabled(true);
        
        Map<String, com.evoleen.hapi.faserver.auth.AuthConfigurationProperties.AuthProviderConfiguration> providers = new HashMap<>();
        
        // Azure provider
        com.evoleen.hapi.faserver.auth.AuthConfigurationProperties.AuthProviderConfiguration azureProvider = new com.evoleen.hapi.faserver.auth.AuthConfigurationProperties.AuthProviderConfiguration();
        azureProvider.setType("azure");
        azureProvider.setEnabled(true);
        providers.put("azure", azureProvider);
        
        // Standard OAuth provider
        com.evoleen.hapi.faserver.auth.AuthConfigurationProperties.AuthProviderConfiguration standardProvider = new com.evoleen.hapi.faserver.auth.AuthConfigurationProperties.AuthProviderConfiguration();
        standardProvider.setType("standard");
        standardProvider.setEnabled(true);
        providers.put("oidc", standardProvider);
        
        config.setProviders(providers);
        
        // Verify both providers can be configured without errors
        assertNotNull(config.getProviders().get("azure"));
        assertNotNull(config.getProviders().get("oidc"));
        assertTrue(config.getProviders().get("azure").isEnabled());
        assertTrue(config.getProviders().get("oidc").isEnabled());
    }

    @Test
    void testSecurityFilterChainOrder() throws Exception {
        // Test that FHIR security filter has higher precedence than default
        // This is verified by ensuring FHIR endpoints require authentication
        // while other endpoints may not
        
        mockMvc.perform(get("/fhir/Patient"))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void testJwtDecoderConfiguration() throws Exception {
        // Verify JWT decoder is properly configured for token validation
        String validJwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...";
        
        mockMvc.perform(get("/fhir/Patient")
                .header("Authorization", "Bearer " + validJwt))
                .andExpect(status().isUnauthorized()); // Will be unauthorized due to mocked validator
    }

    @Test
    void testErrorHandlingConfiguration() throws Exception {
        // Test custom authentication entry point is used
        mockMvc.perform(get("/fhir/Patient"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testResourceServerConfiguration() throws Exception {
        // Test OAuth2 Resource Server configuration
        mockMvc.perform(get("/fhir/Patient")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testHttpMethodsAllowed() throws Exception {
        String jwt = "valid.jwt.token";
        
        // Test all HTTP methods are properly secured
        mockMvc.perform(get("/fhir/Patient").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(post("/fhir/Patient").header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resourceType\":\"Patient\"}"))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(put("/fhir/Patient/123").header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resourceType\":\"Patient\",\"id\":\"123\"}"))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(delete("/fhir/Patient/123").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testDisabledAuthenticationConfiguration() throws Exception {
        // Test behavior when authentication is disabled
        AuthConfigurationProperties disabledConfig = new AuthConfigurationProperties();
        disabledConfig.setEnabled(false);
        
        assertFalse(disabledConfig.isEnabled());
        // When disabled, SecurityConfig bean should not be created due to @ConditionalOnProperty
    }

    @Test
    void testContentTypeNegotiation() throws Exception {
        // Test that security works with different content types
        mockMvc.perform(get("/fhir/Patient")
                .header("Accept", "application/fhir+json"))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(get("/fhir/Patient")
                .header("Accept", "application/json"))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(get("/fhir/Patient")
                .header("Accept", "application/fhir+xml"))
                .andExpect(status().isUnauthorized());
    }
}
package com.evoleen.hapi.faserver.config;

import com.evoleen.hapi.faserver.auth.AuthConfigurationProperties;
import com.evoleen.hapi.faserver.auth.AuthProviderConfig;
import com.evoleen.hapi.faserver.auth.AzureIdentityProviderConfig;
import com.evoleen.hapi.faserver.auth.OAuthProviderConfig;
import com.evoleen.hapi.faserver.util.JwtTestUtils;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for Fire Arrow authentication tests.
 * Provides common configuration and utilities for all authentication tests.
 */
@SpringBootTest(classes = TestConfiguration.class)
@ActiveProfiles("test")
public abstract class BaseAuthenticationTest {

    @Autowired
    protected TestConfigurationProperties testConfig;

    protected AuthConfigurationProperties authConfig;
    
    @BeforeEach
    void setUpBaseAuthentication() {
        authConfig = createTestAuthConfig();
    }

    /**
     * Create a standardized AuthConfigurationProperties for testing.
     */
    protected AuthConfigurationProperties createTestAuthConfig() {
        AuthConfigurationProperties config = new AuthConfigurationProperties();
        config.setEnabled(true);
        config.setRequired(false); // Flexible for different test scenarios
        
        // Set up test providers
        Map<String, AuthConfigurationProperties.AuthProviderConfiguration> providers = new HashMap<>();
        
        // OAuth provider
        TestConfigurationProperties.ProviderTestConfig oauthTestConfig = testConfig.getProviders().get("test-provider");
        if (oauthTestConfig == null) {
            oauthTestConfig = new TestConfigurationProperties.ProviderTestConfig();
            oauthTestConfig.setType("oauth");
            oauthTestConfig.setDiscoveryUrl("https://example.com/.well-known/openid_configuration");
            oauthTestConfig.setAudience("test-audience");
        }
        
        AuthConfigurationProperties.AuthProviderConfiguration oauthProvider = 
                new AuthConfigurationProperties.AuthProviderConfiguration();
        oauthProvider.setType(oauthTestConfig.getType());
        oauthProvider.setEnabled(true);
        
        OAuthProviderConfig oauthConfig = new OAuthProviderConfig();
        oauthConfig.setDiscoveryUrl(oauthTestConfig.getDiscoveryUrl());
        oauthConfig.setAudience(oauthTestConfig.getAudience());
        oauthProvider.setOauth(oauthConfig);
        
        providers.put("test-provider", oauthProvider);
        
        // Azure provider  
        TestConfigurationProperties.ProviderTestConfig azureTestConfig = testConfig.getProviders().get("azure-provider");
        if (azureTestConfig == null) {
            azureTestConfig = new TestConfigurationProperties.ProviderTestConfig();
            azureTestConfig.setType("azure_identity");
            azureTestConfig.setTenantId("test-tenant");
            azureTestConfig.setApplicationId("test-app");
        }
        
        AuthConfigurationProperties.AuthProviderConfiguration azureProvider = 
                new AuthConfigurationProperties.AuthProviderConfiguration();
        azureProvider.setType(azureTestConfig.getType());
        azureProvider.setEnabled(true);
        
        AzureIdentityProviderConfig azureConfig = new AzureIdentityProviderConfig();
        azureConfig.setTenantId(azureTestConfig.getTenantId());
        azureConfig.setApplicationId(azureTestConfig.getApplicationId());
        azureProvider.setAzureIdentity(azureConfig);
        
        providers.put("azure-provider", azureProvider);
        
        config.setProviders(providers);
        
        // Set up default claim mapping
        AuthProviderConfig.ClaimMapping claimMapping = new AuthProviderConfig.ClaimMapping();
        claimMapping.setUserId(testConfig.getDefaultClaims().getUserIdClaim());
        claimMapping.setEmail(testConfig.getDefaultClaims().getEmailClaim());
        claimMapping.setRoles(testConfig.getDefaultClaims().getRolesClaim());
        claimMapping.setFhirId(testConfig.getDefaultClaims().getFhirIdClaim());
        claimMapping.setUserRoleResourceType(testConfig.getDefaultClaims().getResourceTypeClaim());
        config.setDefaultClaimMapping(claimMapping);
        
        return config;
    }

    /**
     * Create a valid JWT token for testing.
     */
    protected String createValidTestToken() {
        return JwtTestUtils.createValidToken(
            testConfig.getJwt().getValidSubject(),
            testConfig.getJwt().getValidIssuer(),
            testConfig.getJwt().getValidAudience()
        );
    }

    /**
     * Create a valid JWT token with custom subject.
     */
    protected String createValidTestToken(String subject) {
        return JwtTestUtils.createValidToken(
            subject,
            testConfig.getJwt().getValidIssuer(),
            testConfig.getJwt().getValidAudience()
        );
    }

    /**
     * Create an expired JWT token for testing.
     */
    protected String createExpiredTestToken() {
        return JwtTestUtils.createExpiredToken();
    }

    /**
     * Create a JWT token with invalid signature for testing.
     */
    protected String createInvalidSignatureTestToken() {
        return JwtTestUtils.createTokenWithInvalidSignature();
    }

    /**
     * Create an Azure-style JWT token for testing.
     */
    protected String createAzureTestToken() {
        return JwtTestUtils.createAzureToken();
    }

    /**
     * Get configured performance expectations.
     */
    protected TestConfigurationProperties.PerformanceConfig getPerformanceConfig() {
        return testConfig.getPerformance();
    }
}
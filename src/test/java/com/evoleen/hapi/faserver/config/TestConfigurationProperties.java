package com.evoleen.hapi.faserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Test configuration properties to abstract test setup values.
 * Provides centralized configuration for all Fire Arrow authentication tests.
 */
@Component
@ConfigurationProperties(prefix = "test.auth")
public class TestConfigurationProperties {

    private JwtTestConfig jwt = new JwtTestConfig();
    private Map<String, ProviderTestConfig> providers = new HashMap<>(); 
    private ClaimMappingConfig defaultClaims = new ClaimMappingConfig();
    private PerformanceConfig performance = new PerformanceConfig();
    
    public JwtTestConfig getJwt() {
        return jwt;
    }
    
    public void setJwt(JwtTestConfig jwt) {
        this.jwt = jwt;
    }
    
    public Map<String, ProviderTestConfig> getProviders() {
        return providers;
    }
    
    public void setProviders(Map<String, ProviderTestConfig> providers) {
        this.providers = providers;
    }
    
    public ClaimMappingConfig getDefaultClaims() {
        return defaultClaims;
    }
    
    public void setDefaultClaims(ClaimMappingConfig defaultClaims) {
        this.defaultClaims = defaultClaims;
    }
    
    public PerformanceConfig getPerformance() {
        return performance;
    }
    
    public void setPerformance(PerformanceConfig performance) {
        this.performance = performance;
    }
    
    public static class JwtTestConfig {
        private String validSubject = "test-user";
        private String validIssuer = "https://example.com";
        private String validAudience = "test-audience";
        private String testSecretKey = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdmFsaWRhdGlvbi10ZXN0aW5nLW9ubHk";
        private long validityDurationMs = 3600000; // 1 hour
        
        // Getters and setters
        public String getValidSubject() { return validSubject; }
        public void setValidSubject(String validSubject) { this.validSubject = validSubject; }
        
        public String getValidIssuer() { return validIssuer; }
        public void setValidIssuer(String validIssuer) { this.validIssuer = validIssuer; }
        
        public String getValidAudience() { return validAudience; }
        public void setValidAudience(String validAudience) { this.validAudience = validAudience; }
        
        public String getTestSecretKey() { return testSecretKey; }
        public void setTestSecretKey(String testSecretKey) { this.testSecretKey = testSecretKey; }
        
        public long getValidityDurationMs() { return validityDurationMs; }
        public void setValidityDurationMs(long validityDurationMs) { this.validityDurationMs = validityDurationMs; }
    }
    
    public static class ProviderTestConfig {
        private String type;
        private String discoveryUrl;
        private String audience;
        private String tenantId;
        private String applicationId;
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getDiscoveryUrl() { return discoveryUrl; }
        public void setDiscoveryUrl(String discoveryUrl) { this.discoveryUrl = discoveryUrl; }
        
        public String getAudience() { return audience; }
        public void setAudience(String audience) { this.audience = audience; }
        
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        
        public String getApplicationId() { return applicationId; }
        public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    }
    
    public static class ClaimMappingConfig {
        private String userIdClaim = "sub";
        private String emailClaim = "email";
        private String rolesClaim = "roles";
        private String fhirIdClaim = "fhir_id";
        private String resourceTypeClaim = "resource_type";
        
        // Getters and setters
        public String getUserIdClaim() { return userIdClaim; }
        public void setUserIdClaim(String userIdClaim) { this.userIdClaim = userIdClaim; }
        
        public String getEmailClaim() { return emailClaim; }
        public void setEmailClaim(String emailClaim) { this.emailClaim = emailClaim; }
        
        public String getRolesClaim() { return rolesClaim; }
        public void setRolesClaim(String rolesClaim) { this.rolesClaim = rolesClaim; }
        
        public String getFhirIdClaim() { return fhirIdClaim; }
        public void setFhirIdClaim(String fhirIdClaim) { this.fhirIdClaim = fhirIdClaim; }
        
        public String getResourceTypeClaim() { return resourceTypeClaim; }
        public void setResourceTypeClaim(String resourceTypeClaim) { this.resourceTypeClaim = resourceTypeClaim; }
    }
    
    public static class PerformanceConfig {
        private long cachedValidationMaxMs = 100;
        private long uncachedValidationMaxMs = 500;
        private int concurrentThreadCount = 10;
        
        // Getters and setters
        public long getCachedValidationMaxMs() { return cachedValidationMaxMs; }
        public void setCachedValidationMaxMs(long cachedValidationMaxMs) { this.cachedValidationMaxMs = cachedValidationMaxMs; }
        
        public long getUncachedValidationMaxMs() { return uncachedValidationMaxMs; }
        public void setUncachedValidationMaxMs(long uncachedValidationMaxMs) { this.uncachedValidationMaxMs = uncachedValidationMaxMs; }
        
        public int getConcurrentThreadCount() { return concurrentThreadCount; }
        public void setConcurrentThreadCount(int concurrentThreadCount) { this.concurrentThreadCount = concurrentThreadCount; }
    }
}
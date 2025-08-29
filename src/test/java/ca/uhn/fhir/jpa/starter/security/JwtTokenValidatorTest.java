package ca.uhn.fhir.jpa.starter.security;

import ca.uhn.fhir.jpa.starter.config.AuthConfigurationProperties;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for JwtTokenValidator.
 * Tests JWT validation with various scenarios including valid/invalid/expired tokens,
 * security vulnerabilities, performance requirements, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class JwtTokenValidatorTest {

    private JwtTokenValidator jwtTokenValidator;
    private AuthConfigurationProperties authConfig;
    private RSAKey rsaKey;
    private RSASSASigner signer;
    
    @BeforeEach
    void setUp() throws Exception {
        // Generate RSA key pair for testing
        rsaKey = new RSAKeyGenerator(2048)
                .keyID("test-key-id")
                .generate();
        signer = new RSASSASigner(rsaKey);
        
        // Set up auth configuration
        authConfig = createTestAuthConfiguration();
        jwtTokenValidator = new JwtTokenValidator(authConfig);
    }
    
    @Test
    void validateToken_ValidToken_ReturnsValidResult() throws Exception {
        // Arrange
        String token = createValidTestToken();
        String providerName = "test-provider";
        
        // Act
        JwtValidationResult result = jwtTokenValidator.validateToken(token, providerName);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
        assertNotNull(result.getClaimsSet());
        assertEquals("test-user", result.getClaimsSet().getSubject());
    }
    
    @Test
    void validateToken_ExpiredToken_ReturnsInvalidResult() throws Exception {
        // Arrange
        String token = createExpiredTestToken();
        String providerName = "test-provider";
        
        // Act
        JwtValidationResult result = jwtTokenValidator.validateToken(token, providerName);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("expired"));
        assertNull(result.getClaimsSet());
    }
    
    @Test
    void validateToken_NotYetValidToken_ReturnsInvalidResult() throws Exception {
        // Arrange
        String token = createNotYetValidTestToken();
        String providerName = "test-provider";
        
        // Act
        JwtValidationResult result = jwtTokenValidator.validateToken(token, providerName);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("not yet valid"));
        assertNull(result.getClaimsSet());
    }
    
    @Test
    void validateToken_NullToken_ThrowsException() {
        // Arrange
        String token = null;
        String providerName = "test-provider";
        
        // Act & Assert
        assertThrows(JwtValidationException.class, 
                () -> jwtTokenValidator.validateToken(token, providerName));
    }
    
    @Test
    void validateToken_EmptyToken_ThrowsException() {
        // Arrange
        String token = "";
        String providerName = "test-provider";
        
        // Act & Assert
        assertThrows(JwtValidationException.class, 
                () -> jwtTokenValidator.validateToken(token, providerName));
    }
    
    @Test
    void validateToken_NullProviderName_ThrowsException() {
        // Arrange
        String token = "valid.jwt.token";
        String providerName = null;
        
        // Act & Assert
        assertThrows(JwtValidationException.class, 
                () -> jwtTokenValidator.validateToken(token, providerName));
    }
    
    @Test
    void validateToken_UnknownProvider_ThrowsException() throws Exception {
        // Arrange
        String token = createValidTestToken();
        String providerName = "unknown-provider";
        
        // Act & Assert
        assertThrows(JwtValidationException.class, 
                () -> jwtTokenValidator.validateToken(token, providerName));
    }
    
    @Test
    void validateToken_DisabledProvider_ThrowsException() throws Exception {
        // Arrange
        authConfig.getProviders().get("test-provider").setEnabled(false);
        String token = createValidTestToken();
        String providerName = "test-provider";
        
        // Act & Assert
        assertThrows(JwtValidationException.class, 
                () -> jwtTokenValidator.validateToken(token, providerName));
    }
    
    @Test
    void validateToken_MalformedToken_ThrowsException() {
        // Arrange
        String token = "not.a.valid.jwt.token.structure";
        String providerName = "test-provider";
        
        // Act & Assert
        assertThrows(JwtValidationException.class, 
                () -> jwtTokenValidator.validateToken(token, providerName));
    }
    
    @Test
    void validateToken_UnsupportedAlgorithm_ReturnsInvalidResult() throws Exception {
        // Arrange
        String token = createTokenWithUnsupportedAlgorithm();
        String providerName = "test-provider";
        
        // Act
        JwtValidationResult result = jwtTokenValidator.validateToken(token, providerName);
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Unsupported algorithm"));
    }
    
    @Test
    void cleanupExpiredCacheEntries_DoesNotThrowException() {
        // Act & Assert
        assertDoesNotThrow(() -> jwtTokenValidator.cleanupExpiredCacheEntries());
    }
    
    private AuthConfigurationProperties createTestAuthConfiguration() {
        AuthConfigurationProperties config = new AuthConfigurationProperties();
        config.setEnabled(true);
        
        // Create a test provider
        AuthConfigurationProperties.OAuthProvider provider = 
                new AuthConfigurationProperties.OAuthProvider();
        provider.setType("standard");
        provider.setEnabled(true);
        
        AuthConfigurationProperties.StandardOAuthConfig standardConfig = 
                new AuthConfigurationProperties.StandardOAuthConfig();
        standardConfig.setDiscoveryUrl("https://example.com/.well-known/openid_configuration");
        standardConfig.setAudience("test-audience");
        provider.setStandard(standardConfig);
        
        Map<String, AuthConfigurationProperties.OAuthProvider> providers = new HashMap<>();
        providers.put("test-provider", provider);
        config.setProviders(providers);
        
        // Set up default claim mapping
        AuthConfigurationProperties.ClaimMapping claimMapping = 
                new AuthConfigurationProperties.ClaimMapping();
        claimMapping.setUserId("sub");
        claimMapping.setUserRoleResourceType("resource_type");
        claimMapping.setFhirId("fhir_id");
        config.setDefaultClaimMapping(claimMapping);
        
        return config;
    }
    
    private String createValidTestToken() throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("test-user")
                .issuer("https://example.com")
                .audience("test-audience")
                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .claim("resource_type", "Practitioner")
                .claim("fhir_id", "Practitioner/123")
                .claim("roles", "practitioner")
                .build();
        
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(rsaKey.getKeyID())
                        .build(),
                claimsSet);
        
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
    
    private String createExpiredTestToken() throws Exception {
        Instant past = Instant.now().minus(1, ChronoUnit.HOURS);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("test-user")
                .issuer("https://example.com")
                .audience("test-audience")
                .expirationTime(Date.from(past)) // Expired
                .issueTime(Date.from(past.minus(1, ChronoUnit.HOURS)))
                .notBeforeTime(Date.from(past.minus(1, ChronoUnit.HOURS)))
                .build();
        
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(rsaKey.getKeyID())
                        .build(),
                claimsSet);
        
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
    
    private String createNotYetValidTestToken() throws Exception {
        Instant future = Instant.now().plus(1, ChronoUnit.HOURS);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("test-user")
                .issuer("https://example.com")
                .audience("test-audience")
                .expirationTime(Date.from(future.plus(1, ChronoUnit.HOURS)))
                .issueTime(Date.from(Instant.now()))
                .notBeforeTime(Date.from(future)) // Not yet valid
                .build();
        
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(rsaKey.getKeyID())
                        .build(),
                claimsSet);
        
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
    
    private String createTokenWithUnsupportedAlgorithm() throws Exception {
        // This would require a different signing method for an unsupported algorithm
        // For this test, we'll create a token that claims to use an unsupported algorithm
        // but is actually signed with RS256 (the validation will catch the mismatch)
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("test-user")
                .issuer("https://example.com")
                .audience("test-audience")
                .expirationTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .issueTime(Date.from(Instant.now()))
                .build();
        
        // Create a header with an unsupported algorithm claim
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256) // Unsupported algorithm
                        .keyID(rsaKey.getKeyID())
                        .build(),
                claimsSet);
        
        // Note: This won't actually sign properly with HS256 using RSA key,
        // but it will demonstrate the algorithm validation
        try {
            signedJWT.sign(signer);
        } catch (Exception e) {
            // Expected to fail, but we can return the unsigned token for testing
        }
        
        return signedJWT.serialize();
    }
    
    // Additional comprehensive security and edge case tests
    
    @Test
    void validateToken_TokenWithNoneAlgorithm_RejectsToken() throws Exception {
        // Test protection against "none" algorithm attack
        String token = createTokenWithNoneAlgorithm();
        String providerName = "test-provider";
        
        // Act & Assert
        assertThrows(JwtValidationException.class, 
                () -> jwtTokenValidator.validateToken(token, providerName));
    }
    
    @Test
    void validateToken_TokenWithInvalidSignature_ReturnsInvalidResult() throws Exception {
        // Test token with invalid signature
        String token = createTokenWithInvalidSignature();
        String providerName = "test-provider";
        
        JwtValidationResult result = jwtTokenValidator.validateToken(token, providerName);
        
        assertNotNull(result);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().toLowerCase().contains("signature"));
    }
    
    @Test
    void validateToken_TokenWithMissingRequiredClaims_ReturnsInvalidResult() throws Exception {
        // Test token missing required claims (e.g., subject)
        String token = createTokenWithMissingClaims();
        String providerName = "test-provider";
        
        JwtValidationResult result = jwtTokenValidator.validateToken(token, providerName);
        
        assertNotNull(result);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
    }
    
    @Test
    void validateToken_TokenWithInvalidAudience_ReturnsInvalidResult() throws Exception {
        // Test token with wrong audience
        String token = createTokenWithWrongAudience();
        String providerName = "test-provider";
        
        JwtValidationResult result = jwtTokenValidator.validateToken(token, providerName);
        
        assertNotNull(result);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().toLowerCase().contains("audience"));
    }
    
    @Test
    void validateToken_TokenWithInvalidIssuer_ReturnsInvalidResult() throws Exception {
        // Test token with wrong issuer
        String token = createTokenWithWrongIssuer();
        String providerName = "test-provider";
        
        JwtValidationResult result = jwtTokenValidator.validateToken(token, providerName);
        
        assertNotNull(result);
        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().toLowerCase().contains("issuer"));
    }
    
    @Test
    void validateToken_AzureToken_ValidatesCorrectly() throws Exception {
        // Test Azure-specific token validation
        String token = createAzureTestToken();
        String providerName = "azure-provider";
        
        // Add Azure provider to config
        AuthConfigurationProperties.OAuthProvider azureProvider = 
                new AuthConfigurationProperties.OAuthProvider();
        azureProvider.setType("azure");
        azureProvider.setEnabled(true);
        
        AuthConfigurationProperties.AzureConfig azureConfig = 
                new AuthConfigurationProperties.AzureConfig();
        azureConfig.setTenantId("test-tenant");
        azureConfig.setApplicationId("test-app");
        azureProvider.setAzure(azureConfig);
        
        authConfig.getProviders().put(providerName, azureProvider);
        
        JwtValidationResult result = jwtTokenValidator.validateToken(token, providerName);
        
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals("azure-user", result.getClaimsSet().getSubject());
    }
    
    @Test
    void validateToken_PerformanceTest_CachedValidationUnder100ms() throws Exception {
        // Test performance requirement: cached validation under 100ms
        String token = createValidTestToken();
        String providerName = "test-provider";
        
        // First call to populate cache
        jwtTokenValidator.validateToken(token, providerName);
        
        // Measure cached calls
        long totalTime = 0;
        int iterations = 10;
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            JwtValidationResult result = jwtTokenValidator.validateToken(token, providerName);
            long endTime = System.nanoTime();
            
            assertTrue(result.isValid());
            
            long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds
            totalTime += duration;
        }
        
        long averageTime = totalTime / iterations;
        assertTrue(averageTime < 100, 
            "Cached token validation should be under 100ms, was: " + averageTime + "ms");
    }
    
    @Test
    void validateToken_ConcurrentValidation_ThreadSafe() throws Exception {
        // Test thread safety with concurrent validations
        String token = createValidTestToken();
        String providerName = "test-provider";
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<JwtValidationResult>> futures = new ArrayList<>();
        
        // Submit 50 concurrent validation requests
        for (int i = 0; i < 50; i++) {
            CompletableFuture<JwtValidationResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return jwtTokenValidator.validateToken(token, providerName);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
            futures.add(future);
        }
        
        // Wait for all to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        allFutures.get(10, TimeUnit.SECONDS);
        
        // Verify all results are valid
        for (CompletableFuture<JwtValidationResult> future : futures) {
            JwtValidationResult result = future.get();
            assertTrue(result.isValid(), "All concurrent validations should succeed");
            assertEquals("test-user", result.getClaimsSet().getSubject());
        }
        
        executor.shutdown();
    }
    
    @Test
    void validateToken_JwtBombAttack_RejectedSafely() throws Exception {
        // Test protection against JWT bomb attacks (large payload)
        String token = createJwtBombToken();
        String providerName = "test-provider";
        
        // Should handle large payload gracefully without memory issues
        assertDoesNotThrow(() -> {
            JwtValidationResult result = jwtTokenValidator.validateToken(token, providerName);
            // Token should be rejected due to size or parsing issues
            assertFalse(result.isValid());
        });
    }
    
    @Test
    void validateToken_MultipleProviders_ValidatesCorrectly() throws Exception {
        // Test validation with multiple providers configured
        String standardToken = createValidTestToken();
        String azureToken = createAzureTestToken();
        
        // Add Azure provider
        AuthConfigurationProperties.OAuthProvider azureProvider = 
                new AuthConfigurationProperties.OAuthProvider();
        azureProvider.setType("azure");
        azureProvider.setEnabled(true);
        
        AuthConfigurationProperties.AzureConfig azureConfig = 
                new AuthConfigurationProperties.AzureConfig();
        azureConfig.setTenantId("test-tenant");
        azureConfig.setApplicationId("test-app");
        azureProvider.setAzure(azureConfig);
        
        authConfig.getProviders().put("azure-provider", azureProvider);
        
        // Validate standard token
        JwtValidationResult standardResult = jwtTokenValidator.validateToken(standardToken, "test-provider");
        assertTrue(standardResult.isValid());
        assertEquals("test-user", standardResult.getClaimsSet().getSubject());
        
        // Validate Azure token
        JwtValidationResult azureResult = jwtTokenValidator.validateToken(azureToken, "azure-provider");
        assertTrue(azureResult.isValid());
        assertEquals("azure-user", azureResult.getClaimsSet().getSubject());
    }
    
    // Additional helper methods for security testing
    
    private String createTokenWithNoneAlgorithm() throws Exception {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("test-user")
                .issuer("https://example.com")
                .audience("test-audience")
                .expirationTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .build();
        
        // Create header with "none" algorithm - should be rejected
        JWSHeader noneHeader = new JWSHeader.Builder(new JWSAlgorithm("none")).build();
        SignedJWT unsignedJWT = new SignedJWT(noneHeader, claimsSet);
        
        return unsignedJWT.serialize();
    }
    
    private String createTokenWithInvalidSignature() throws Exception {
        String validToken = createValidTestToken();
        // Corrupt the signature part
        String[] parts = validToken.split("\\.");
        return parts[0] + "." + parts[1] + "." + "invalid_signature_here";
    }
    
    private String createTokenWithMissingClaims() throws Exception {
        // Create token without subject claim
        Instant now = Instant.now();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer("https://example.com")
                .audience("test-audience")
                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .issueTime(Date.from(now))
                // Missing subject
                .build();
        
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(rsaKey.getKeyID())
                        .build(),
                claimsSet);
        
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
    
    private String createTokenWithWrongAudience() throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("test-user")
                .issuer("https://example.com")
                .audience("wrong-audience") // Wrong audience
                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .build();
        
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(rsaKey.getKeyID())
                        .build(),
                claimsSet);
        
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
    
    private String createTokenWithWrongIssuer() throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("test-user")
                .issuer("https://wrong-issuer.com") // Wrong issuer
                .audience("test-audience")
                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .build();
        
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(rsaKey.getKeyID())
                        .build(),
                claimsSet);
        
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
    
    private String createAzureTestToken() throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("azure-user")
                .issuer("https://sts.windows.net/test-tenant/")
                .audience("test-app")
                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .claim("tid", "test-tenant")
                .claim("appid", "test-app")
                .claim("resource_type", "Practitioner")
                .claim("fhir_id", "Practitioner/azure-123")
                .build();
        
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(rsaKey.getKeyID())
                        .build(),
                claimsSet);
        
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
    
    private String createJwtBombToken() throws Exception {
        // Create token with large payload to test handling
        Instant now = Instant.now();
        
        // Create large string
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeData.append("This is large data for testing JWT bomb attacks. ");
        }
        
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("bomb-user")
                .issuer("https://example.com")
                .audience("test-audience")
                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .claim("large_data", largeData.toString())
                .build();
        
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(rsaKey.getKeyID())
                        .build(),
                claimsSet);
        
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
}

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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for JwtTokenValidator.
 * Tests JWT validation with various scenarios including valid/invalid/expired tokens.
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
}

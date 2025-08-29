package ca.uhn.fhir.jpa.starter.security;

import ca.uhn.fhir.jpa.starter.config.AuthConfigurationProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * JWT Token Validator that supports multiple OAuth providers with configurable algorithms.
 * Provides high-performance validation with caching and proper error handling.
 * 
 * Performance target: <100ms cached validation
 * Supported algorithms: RS256, RS384, RS512
 * Supports: Azure Identity, Standard OAuth/OIDC
 */
@Component
@ConditionalOnProperty(prefix = "hapi.fhir.auth", name = "enabled", havingValue = "true")
public class JwtTokenValidator {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenValidator.class);
    
    private static final Set<JWSAlgorithm> SUPPORTED_ALGORITHMS = Set.of(
            JWSAlgorithm.RS256, JWSAlgorithm.RS384, JWSAlgorithm.RS512
    );
    
    private static final long JWK_CACHE_TTL_MINUTES = 30;
    private static final long VALIDATION_CACHE_TTL_MINUTES = 5;
    
    private final AuthConfigurationProperties authConfig;
    
    // Caching for JWK sets and validation results
    private final Map<String, CachedJWKSet> jwkSetCache = new ConcurrentHashMap<>();
    private final Map<String, CachedValidationResult> validationCache = new ConcurrentHashMap<>();
    
    public JwtTokenValidator(AuthConfigurationProperties authConfig) {
        this.authConfig = authConfig;
    }
    
    /**
     * Validates a JWT token for a specific provider
     * 
     * @param token JWT token string
     * @param providerName name of the OAuth provider
     * @return validation result containing claims and validity status
     * @throws JwtValidationException if validation fails
     */
    public JwtValidationResult validateToken(String token, String providerName) throws JwtValidationException {
        if (!StringUtils.hasText(token)) {
            throw new JwtValidationException("Token is null or empty");
        }
        
        if (!StringUtils.hasText(providerName)) {
            throw new JwtValidationException("Provider name is null or empty");
        }
        
        // Check validation cache first
        String cacheKey = generateCacheKey(token, providerName);
        CachedValidationResult cachedResult = validationCache.get(cacheKey);
        if (cachedResult != null && !cachedResult.isExpired()) {
            return cachedResult.getResult();
        }
        
        try {
            JwtValidationResult result = performTokenValidation(token, providerName);
            
            // Cache successful validation results
            if (result.isValid()) {
                validationCache.put(cacheKey, new CachedValidationResult(result, 
                        Instant.now().plusSeconds(TimeUnit.MINUTES.toSeconds(VALIDATION_CACHE_TTL_MINUTES))));
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("JWT validation failed for provider {}: {}", providerName, e.getMessage());
            throw new JwtValidationException("Token validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Performs the actual token validation
     */
    private JwtValidationResult performTokenValidation(String token, String providerName) throws JwtValidationException {
        AuthConfigurationProperties.OAuthProvider provider = authConfig.getProviders().get(providerName);
        if (provider == null || !provider.isEnabled()) {
            throw new JwtValidationException("Provider " + providerName + " is not configured or disabled");
        }
        
        try {
            // Parse the JWT token
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            
            // Validate token expiration
            Date expirationTime = claimsSet.getExpirationTime();
            if (expirationTime != null && expirationTime.before(new Date())) {
                return JwtValidationResult.invalid("Token has expired");
            }
            
            // Validate not before time
            Date notBeforeTime = claimsSet.getNotBeforeTime();
            if (notBeforeTime != null && notBeforeTime.after(new Date())) {
                return JwtValidationResult.invalid("Token is not yet valid");
            }
            
            // Validate algorithm
            JWSAlgorithm algorithm = signedJWT.getHeader().getAlgorithm();
            if (!SUPPORTED_ALGORITHMS.contains(algorithm)) {
                return JwtValidationResult.invalid("Unsupported algorithm: " + algorithm);
            }
            
            // Get JWK set for signature verification
            JWKSet jwkSet = getJWKSet(provider);
            if (jwkSet == null) {
                return JwtValidationResult.invalid("Unable to retrieve JWK set for provider");
            }
            
            // Verify signature
            boolean signatureValid = verifySignature(signedJWT, jwkSet);
            if (!signatureValid) {
                return JwtValidationResult.invalid("Invalid token signature");
            }
            
            // Validate audience if configured
            if (!validateAudience(claimsSet, provider)) {
                return JwtValidationResult.invalid("Invalid audience");
            }
            
            // Validate issuer
            if (!validateIssuer(claimsSet, provider)) {
                return JwtValidationResult.invalid("Invalid issuer");
            }
            
            return JwtValidationResult.valid(claimsSet);
            
        } catch (ParseException e) {
            throw new JwtValidationException("Failed to parse JWT token", e);
        }
    }
    
    /**
     * Retrieves JWK set for a provider with caching
     */
    private JWKSet getJWKSet(AuthConfigurationProperties.OAuthProvider provider) throws JwtValidationException {
        String jwksUri = getJwksUri(provider);
        if (!StringUtils.hasText(jwksUri)) {
            throw new JwtValidationException("Unable to determine JWKS URI for provider");
        }
        
        // Check cache first
        CachedJWKSet cachedJWKSet = jwkSetCache.get(jwksUri);
        if (cachedJWKSet != null && !cachedJWKSet.isExpired()) {
            return cachedJWKSet.getJwkSet();
        }
        
        try {
            // Fetch JWK set from the provider
            JWKSet jwkSet = JWKSet.load(new URL(jwksUri));
            
            // Cache the JWK set
            jwkSetCache.put(jwksUri, new CachedJWKSet(jwkSet, 
                    Instant.now().plusSeconds(TimeUnit.MINUTES.toSeconds(JWK_CACHE_TTL_MINUTES))));
            
            return jwkSet;
            
        } catch (IOException | ParseException e) {
            throw new JwtValidationException("Failed to load JWK set from " + jwksUri, e);
        }
    }
    
    /**
     * Gets the JWKS URI for a provider
     */
    private String getJwksUri(AuthConfigurationProperties.OAuthProvider provider) {
        if ("azure".equalsIgnoreCase(provider.getType()) && provider.getAzure() != null) {
            // Azure AD JWKS URI format
            String instance = provider.getAzure().getInstance();
            String tenantId = provider.getAzure().getTenantId();
            return instance + tenantId + "/discovery/v2.0/keys";
        } else if ("standard".equalsIgnoreCase(provider.getType()) && provider.getStandard() != null) {
            // For standard OIDC, we need to discover the JWKS URI
            return discoverJwksUri(provider.getStandard().getDiscoveryUrl());
        }
        
        return null;
    }
    
    /**
     * Discovers JWKS URI from OIDC discovery URL
     */
    private String discoverJwksUri(String discoveryUrl) {
        try {
            // This is a simplified implementation - in production, you would
            // properly parse the OIDC discovery document
            if (discoveryUrl.endsWith("/.well-known/openid_configuration")) {
                // For now, assume the JWKS URI follows standard pattern
                String baseUrl = discoveryUrl.replace("/.well-known/openid_configuration", "");
                return baseUrl + "/.well-known/jwks.json";
            }
            return discoveryUrl.replace("/.well-known/openid_configuration", "/.well-known/jwks.json");
        } catch (Exception e) {
            logger.error("Failed to discover JWKS URI from {}: {}", discoveryUrl, e.getMessage());
            return null;
        }
    }
    
    /**
     * Verifies JWT signature using JWK set
     */
    private boolean verifySignature(SignedJWT signedJWT, JWKSet jwkSet) {
        try {
            String keyId = signedJWT.getHeader().getKeyID();
            
            for (JWK jwk : jwkSet.getKeys()) {
                if (keyId == null || keyId.equals(jwk.getKeyID())) {
                    if (jwk instanceof RSAKey) {
                        RSAKey rsaKey = (RSAKey) jwk;
                        JWSVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());
                        if (signedJWT.verify(verifier)) {
                            return true;
                        }
                    }
                }
            }
            return false;
            
        } catch (JOSEException e) {
            logger.error("Error verifying JWT signature: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Validates JWT audience claim
     */
    private boolean validateAudience(JWTClaimsSet claimsSet, AuthConfigurationProperties.OAuthProvider provider) {
        try {
            List<String> audiences = claimsSet.getAudience();
            if (audiences == null || audiences.isEmpty()) {
                return true; // No audience validation required
            }
            
            String expectedAudience = getExpectedAudience(provider);
            if (!StringUtils.hasText(expectedAudience)) {
                return true; // No expected audience configured
            }
            
            return audiences.contains(expectedAudience);
            
        } catch (Exception e) {
            logger.error("Error validating audience: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets expected audience for a provider
     */
    private String getExpectedAudience(AuthConfigurationProperties.OAuthProvider provider) {
        if ("azure".equalsIgnoreCase(provider.getType()) && provider.getAzure() != null) {
            return provider.getAzure().getApplicationId();
        } else if ("standard".equalsIgnoreCase(provider.getType()) && provider.getStandard() != null) {
            return provider.getStandard().getAudience();
        }
        return null;
    }
    
    /**
     * Validates JWT issuer claim
     */
    private boolean validateIssuer(JWTClaimsSet claimsSet, AuthConfigurationProperties.OAuthProvider provider) {
        try {
            String issuer = claimsSet.getIssuer();
            if (!StringUtils.hasText(issuer)) {
                return false; // Issuer is required
            }
            
            String expectedIssuer = getExpectedIssuer(provider);
            if (!StringUtils.hasText(expectedIssuer)) {
                return true; // No expected issuer configured
            }
            
            return issuer.equals(expectedIssuer);
            
        } catch (Exception e) {
            logger.error("Error validating issuer: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets expected issuer for a provider
     */
    private String getExpectedIssuer(AuthConfigurationProperties.OAuthProvider provider) {
        if ("azure".equalsIgnoreCase(provider.getType()) && provider.getAzure() != null) {
            return provider.getAzure().getInstance() + provider.getAzure().getTenantId() + "/v2.0";
        }
        // For standard OAuth, issuer validation is typically handled by discovery document
        return null;
    }
    
    /**
     * Generates cache key for token validation results
     */
    private String generateCacheKey(String token, String providerName) {
        // Use token hash to avoid storing full token in cache key
        return providerName + ":" + Integer.toHexString(token.hashCode());
    }
    
    /**
     * Clears expired entries from caches
     */
    public void cleanupExpiredCacheEntries() {
        Instant now = Instant.now();
        
        jwkSetCache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
        validationCache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }
    
    /**
     * Cached JWK set with expiration
     */
    private static class CachedJWKSet {
        private final JWKSet jwkSet;
        private final Instant expiration;
        
        public CachedJWKSet(JWKSet jwkSet, Instant expiration) {
            this.jwkSet = jwkSet;
            this.expiration = expiration;
        }
        
        public JWKSet getJwkSet() {
            return jwkSet;
        }
        
        public boolean isExpired() {
            return isExpired(Instant.now());
        }
        
        public boolean isExpired(Instant now) {
            return now.isAfter(expiration);
        }
    }
    
    /**
     * Cached validation result with expiration
     */
    private static class CachedValidationResult {
        private final JwtValidationResult result;
        private final Instant expiration;
        
        public CachedValidationResult(JwtValidationResult result, Instant expiration) {
            this.result = result;
            this.expiration = expiration;
        }
        
        public JwtValidationResult getResult() {
            return result;
        }
        
        public boolean isExpired() {
            return isExpired(Instant.now());
        }
        
        public boolean isExpired(Instant now) {
            return now.isAfter(expiration);
        }
    }
}

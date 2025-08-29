package com.evoleen.hapi.faserver.auth;

import com.evoleen.hapi.faserver.security.*;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;

/**
 * Standard OAuth/OIDC authentication provider implementation.
 * Validates JWT tokens using OIDC discovery and JWK sets.
 */
public class OAuthProvider implements AuthProvider {

    private static final Logger logger = LoggerFactory.getLogger(OAuthProvider.class);

    private final String name;
    private final OAuthProviderConfig config;
    private final TokenClaimExtractor claimExtractor;

    public OAuthProvider(String name, OAuthProviderConfig config, TokenClaimExtractor claimExtractor) {
        this.name = name;
        this.config = config;
        this.claimExtractor = claimExtractor;
    }

    @Override
    public JwtValidationResult validateToken(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            JWTClaimsSet claims = jwt.getJWTClaimsSet();

            // Validate token expiration
            if (claims.getExpirationTime() != null && 
                claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
                return JwtValidationResult.failure("Token has expired");
            }

            // Validate audience
            List<String> audiences = claims.getAudience();
            if (audiences == null || !audiences.contains(config.getAudience())) {
                return JwtValidationResult.failure("Invalid audience");
            }

            // Get JWK set and validate signature
            JWKSet jwkSet = getJWKSet(config.getDiscoveryUrl());
            if (!validateSignature(jwt, jwkSet)) {
                return JwtValidationResult.failure("Invalid token signature");
            }

            // Extract user identity from claims
            UserIdentity userIdentity = claimExtractor.extractUserIdentity(claims, name);
            return JwtValidationResult.success(userIdentity, name);

        } catch (ParseException e) {
            logger.warn("Failed to parse JWT token", e);
            return JwtValidationResult.failure("Invalid token format");
        } catch (Exception e) {
            logger.error("JWT validation failed", e);
            return JwtValidationResult.failure("Token validation failed");
        }
    }

    @Cacheable(value = "jwkSets", key = "#discoveryUrl")
    private JWKSet getJWKSet(String discoveryUrl) throws IOException, ParseException {
        // Extract the base URL from discovery URL to construct JWK URI
        String baseUrl = discoveryUrl.replace("/.well-known/openid-configuration", "");
        String jwkSetUrl = baseUrl + "/.well-known/jwks";
        return JWKSet.load(new URL(jwkSetUrl));
    }

    private boolean validateSignature(SignedJWT jwt, JWKSet jwkSet) {
        try {
            String keyId = jwt.getHeader().getKeyID();
            JWK jwk = jwkSet.getKeyByKeyId(keyId);
            
            if (jwk == null) {
                logger.warn("No JWK found for key ID: {}", keyId);
                return false;
            }

            if (jwk instanceof RSAKey) {
                RSAKey rsaKey = (RSAKey) jwk;
                JWSVerifier verifier = new RSASSAVerifier(rsaKey);
                return jwt.verify(verifier);
            }

            logger.warn("Unsupported key type: {}", jwk.getKeyType());
            return false;

        } catch (JOSEException e) {
            logger.warn("Signature validation failed", e);
            return false;
        }
    }

    @Override
    public String getType() {
        return "oauth";
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public String getName() {
        return name;
    }
}

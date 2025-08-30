package com.evoleen.hapi.faserver.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWT test utilities for creating valid and invalid JWT tokens for testing.
 */
public class JwtTestUtils {

    public static final String TEST_SECRET_KEY = "dGVzdC1zZWNyZXQta2V5LWZvci1qd3QtdmFsaWRhdGlvbi10ZXN0aW5nLW9ubHk";
    public static final String VALID_ISSUER = "https://example.com";
    public static final String VALID_AUDIENCE = "test-audience";
    public static final String VALID_SUBJECT = "test-user";

    /**
     * Create a valid JWT token for testing with standard claims.
     */
    public static String createValidToken() {
        return createValidToken(VALID_SUBJECT, VALID_ISSUER, VALID_AUDIENCE);
    }

    /**
     * Create a valid JWT token with custom subject, issuer, and audience.
     */
    public static String createValidToken(String subject, String issuer, String audience) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issuer(issuer)
                    .audience(audience)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("email", subject + "@example.com")
                    .claim("roles", Arrays.asList("practitioner"))
                    .claim("fhir_id", "Practitioner/" + subject)
                    .claim("resource_type", "Practitioner")
                    .build();

            return signToken(claimsSet, TEST_SECRET_KEY);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create valid JWT token", e);
        }
    }

    /**
     * Create an expired JWT token.
     */
    public static String createExpiredToken() {
        try {
            Instant pastTime = Instant.now().minus(2, ChronoUnit.HOURS);
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(VALID_SUBJECT)
                    .issuer(VALID_ISSUER)
                    .audience(VALID_AUDIENCE)
                    .issueTime(Date.from(pastTime.minus(1, ChronoUnit.HOURS)))
                    .expirationTime(Date.from(pastTime))
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            return signToken(claimsSet, TEST_SECRET_KEY);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create expired JWT token", e);
        }
    }

    /**
     * Create a JWT token with invalid signature.
     */
    public static String createTokenWithInvalidSignature() {
        String validToken = createValidToken();
        // Corrupt the signature by changing the last character
        return validToken.substring(0, validToken.length() - 1) + "X";
    }

    /**
     * Create a JWT token with invalid issuer.
     */
    public static String createTokenWithInvalidIssuer() {
        return createValidToken(VALID_SUBJECT, "https://invalid-issuer.com", VALID_AUDIENCE);
    }

    /**
     * Create a JWT token with invalid audience.
     */
    public static String createTokenWithInvalidAudience() {
        return createValidToken(VALID_SUBJECT, VALID_ISSUER, "invalid-audience");
    }

    /**
     * Create a JWT token missing required claims.
     */
    public static String createTokenWithMissingClaims() {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(VALID_SUBJECT)
                    .issuer(VALID_ISSUER)
                    // Missing audience
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                    .build();

            return signToken(claimsSet, TEST_SECRET_KEY);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create token with missing claims", e);
        }
    }

    /**
     * Create a JWT token with custom claims.
     */
    public static String createTokenWithCustomClaims(String subject, List<String> roles, String fhirId, String resourceType) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issuer(VALID_ISSUER)
                    .audience(VALID_AUDIENCE)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("email", subject + "@example.com")
                    .claim("roles", roles)
                    .claim("fhir_id", fhirId)
                    .claim("resource_type", resourceType)
                    .build();

            return signToken(claimsSet, TEST_SECRET_KEY);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create token with custom claims", e);
        }
    }

    /**
     * Create an Azure-style JWT token.
     */
    public static String createAzureToken() {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject("azure-user")
                    .issuer("https://login.microsoftonline.com/test-tenant/v2.0")
                    .audience("test-app")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("email", "azure-user@tenant.onmicrosoft.com")
                    .claim("roles", Arrays.asList("practitioner"))
                    .claim("tid", "test-tenant")
                    .claim("aud", "test-app")
                    .build();

            return signToken(claimsSet, TEST_SECRET_KEY);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Azure JWT token", e);
        }
    }

    /**
     * Create a malformed JWT token (not proper JWT format).
     */
    public static String createMalformedToken() {
        return "not.a.valid.jwt.token.format";
    }

    /**
     * Sign a JWT claims set with the given secret key.
     */
    private static String signToken(JWTClaimsSet claimsSet, String secretKey) throws JOSEException {
        JWSSigner signer = new MACSigner(secretKey.getBytes());
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    /**
     * Extract claims set from a JWT token string for testing assertions.
     */
    public static JWTClaimsSet extractClaimsForTesting(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract claims from token", e);
        }
    }
}
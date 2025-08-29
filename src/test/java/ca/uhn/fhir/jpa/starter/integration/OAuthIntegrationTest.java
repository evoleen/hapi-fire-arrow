package ca.uhn.fhir.jpa.starter.integration;

import ca.uhn.fhir.jpa.starter.auth.AuthConfigurationProperties;
import ca.uhn.fhir.jpa.starter.security.*;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive OAuth 2.0 JWT integration tests.
 * Tests end-to-end OAuth flow including token validation, user authentication,
 * authorization, performance, and security vulnerability scenarios.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "hapi.fhir.auth.enabled=true",
    "hapi.fhir.auth.required=true",
    "server.error.include-message=always"
})
class OAuthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuthConfigurationProperties authConfig;

    private RSAKey rsaKey;
    private RSASSASigner signer;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        baseUrl = "http://localhost:" + port;
        
        // Generate RSA key pair for testing
        rsaKey = new RSAKeyGenerator(2048)
                .keyID("integration-test-key")
                .generate();
        signer = new RSASSASigner(rsaKey);
    }

    @Test
    void testCompleteOAuthFlow_StandardProvider() throws Exception {
        // Test complete OAuth flow with standard provider
        String token = createValidToken("standard-provider");
        
        // Test authenticated access to FHIR endpoints
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/fhir+json");
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/fhir/Patient", 
            HttpMethod.GET, 
            entity, 
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testCompleteOAuthFlow_AzureProvider() throws Exception {
        // Test complete OAuth flow with Azure provider
        String token = createValidAzureToken();
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/fhir+json");
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/fhir/Patient", 
            HttpMethod.GET, 
            entity, 
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testTokenExpiration_RejectsExpiredTokens() throws Exception {
        // Test that expired tokens are properly rejected
        String expiredToken = createExpiredToken();
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + expiredToken);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/fhir/Patient", 
            HttpMethod.GET, 
            entity, 
            String.class
        );
        
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testTokenRefreshScenario() throws Exception {
        // Test token refresh scenario - simulate expired token followed by new token
        String expiredToken = createExpiredToken();
        String newToken = createValidToken("test-provider");
        
        // First request with expired token should fail
        HttpHeaders expiredHeaders = new HttpHeaders();
        expiredHeaders.set("Authorization", "Bearer " + expiredToken);
        HttpEntity<String> expiredEntity = new HttpEntity<>(expiredHeaders);
        
        ResponseEntity<String> expiredResponse = restTemplate.exchange(
            baseUrl + "/fhir/Patient", 
            HttpMethod.GET, 
            expiredEntity, 
            String.class
        );
        assertEquals(HttpStatus.UNAUTHORIZED, expiredResponse.getStatusCode());
        
        // Second request with new token should succeed
        HttpHeaders newHeaders = new HttpHeaders();
        newHeaders.set("Authorization", "Bearer " + newToken);
        HttpEntity<String> newEntity = new HttpEntity<>(newHeaders);
        
        ResponseEntity<String> newResponse = restTemplate.exchange(
            baseUrl + "/fhir/Patient", 
            HttpMethod.GET, 
            newEntity, 
            String.class
        );
        assertEquals(HttpStatus.OK, newResponse.getStatusCode());
    }

    @Test
    void testInvalidTokenScenarios() throws Exception {
        // Test various invalid token scenarios
        String[] invalidTokens = {
            null,
            "",
            "invalid-token",
            "Bearer",
            "Bearer ",
            "Bearer invalid.jwt.token",
            "Basic dGVzdDp0ZXN0", // Basic auth instead of Bearer
            "Bearer " + createMalformedToken(),
            "Bearer " + createTokenWithInvalidSignature()
        };
        
        for (String invalidToken : invalidTokens) {
            HttpHeaders headers = new HttpHeaders();
            if (invalidToken != null && !invalidToken.isEmpty()) {
                if (invalidToken.startsWith("Bearer ")) {
                    headers.set("Authorization", invalidToken);
                } else {
                    headers.set("Authorization", "Bearer " + invalidToken);
                }
            }
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/fhir/Patient", 
                HttpMethod.GET, 
                entity, 
                String.class
            );
            
            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), 
                "Token should be rejected: " + invalidToken);
        }
    }

    @Test
    void testMultipleProviderSupport() throws Exception {
        // Test that multiple OAuth providers can be used simultaneously
        String standardToken = createValidToken("standard-provider");
        String azureToken = createValidAzureToken();
        
        // Test standard provider
        HttpHeaders standardHeaders = new HttpHeaders();
        standardHeaders.set("Authorization", "Bearer " + standardToken);
        HttpEntity<String> standardEntity = new HttpEntity<>(standardHeaders);
        
        ResponseEntity<String> standardResponse = restTemplate.exchange(
            baseUrl + "/fhir/Patient", 
            HttpMethod.GET, 
            standardEntity, 
            String.class
        );
        assertEquals(HttpStatus.OK, standardResponse.getStatusCode());
        
        // Test Azure provider
        HttpHeaders azureHeaders = new HttpHeaders();
        azureHeaders.set("Authorization", "Bearer " + azureToken);
        HttpEntity<String> azureEntity = new HttpEntity<>(azureHeaders);
        
        ResponseEntity<String> azureResponse = restTemplate.exchange(
            baseUrl + "/fhir/Patient", 
            HttpMethod.GET, 
            azureEntity, 
            String.class
        );
        assertEquals(HttpStatus.OK, azureResponse.getStatusCode());
    }

    @Test
    void testPerformanceRequirement_CachedValidationUnder100ms() throws Exception {
        // Test that cached token validation meets <100ms requirement
        String token = createValidToken("test-provider");
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        // First request to populate cache
        restTemplate.exchange(
            baseUrl + "/fhir/Patient", 
            HttpMethod.GET, 
            entity, 
            String.class
        );
        
        // Measure subsequent cached requests
        long totalTime = 0;
        int iterations = 10;
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/fhir/Patient", 
                HttpMethod.GET, 
                entity, 
                String.class
            );
            
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds
            
            totalTime += duration;
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
        
        long averageTime = totalTime / iterations;
        assertTrue(averageTime < 100, 
            "Cached token validation should be under 100ms, was: " + averageTime + "ms");
    }

    @Test
    void testConcurrentAuthenticationRequests() throws Exception {
        // Test concurrent authentication requests for performance and thread safety
        String token = createValidToken("test-provider");
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        // Submit 20 concurrent requests
        for (int i = 0; i < 20; i++) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer " + token);
                    HttpEntity<String> entity = new HttpEntity<>(headers);
                    
                    ResponseEntity<String> response = restTemplate.exchange(
                        baseUrl + "/fhir/Patient", 
                        HttpMethod.GET, 
                        entity, 
                        String.class
                    );
                    
                    return response.getStatusCode() == HttpStatus.OK;
                } catch (Exception e) {
                    return false;
                }
            }, executor);
            
            futures.add(future);
        }
        
        // Wait for all requests to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        allFutures.get(10, TimeUnit.SECONDS);
        
        // Verify all requests succeeded
        for (CompletableFuture<Boolean> future : futures) {
            assertTrue(future.get(), "All concurrent requests should succeed");
        }
        
        executor.shutdown();
    }

    @Test
    void testSecurityVulnerabilityAssessment_JwtBombAttack() throws Exception {
        // Test protection against JWT bomb attacks (deeply nested JSON)
        String maliciousToken = createJwtBombToken();
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + maliciousToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/fhir/Patient", 
            HttpMethod.GET, 
            entity, 
            String.class
        );
        
        // Should be rejected due to malicious structure
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testSecurityVulnerabilityAssessment_AlgorithmConfusion() throws Exception {
        // Test protection against algorithm confusion attacks
        String noneAlgToken = createTokenWithNoneAlgorithm();
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + noneAlgToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/fhir/Patient", 
            HttpMethod.GET, 
            entity, 
            String.class
        );
        
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void testFhirResourceAuthorization() throws Exception {
        // Test FHIR resource-level authorization
        String practitionerToken = createTokenWithRole("Practitioner", "practitioner");
        String patientToken = createTokenWithRole("Patient", "patient");
        
        // Practitioner should have access to Patient resources
        HttpHeaders practitionerHeaders = new HttpHeaders();
        practitionerHeaders.set("Authorization", "Bearer " + practitionerToken);
        HttpEntity<String> practitionerEntity = new HttpEntity<>(practitionerHeaders);
        
        ResponseEntity<String> practitionerResponse = restTemplate.exchange(
            baseUrl + "/fhir/Patient", 
            HttpMethod.GET, 
            practitionerEntity, 
            String.class
        );
        assertEquals(HttpStatus.OK, practitionerResponse.getStatusCode());
        
        // Patient should have limited access (depending on authorization rules)
        HttpHeaders patientHeaders = new HttpHeaders();
        patientHeaders.set("Authorization", "Bearer " + patientToken);
        HttpEntity<String> patientEntity = new HttpEntity<>(patientHeaders);
        
        ResponseEntity<String> patientResponse = restTemplate.exchange(
            baseUrl + "/fhir/Patient", 
            HttpMethod.GET, 
            patientEntity, 
            String.class
        );
        // Result depends on authorization configuration
        assertTrue(patientResponse.getStatusCode() == HttpStatus.OK || 
                  patientResponse.getStatusCode() == HttpStatus.FORBIDDEN);
    }

    @Test
    void testPublicEndpointsAccessibility() throws Exception {
        // Test that public endpoints remain accessible without authentication
        String[] publicEndpoints = {
            "/metadata",
            "/fhir/metadata", 
            "/actuator/health"
        };
        
        for (String endpoint : publicEndpoints) {
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + endpoint, 
                String.class
            );
            
            assertEquals(HttpStatus.OK, response.getStatusCode(), 
                "Public endpoint should be accessible: " + endpoint);
        }
    }

    @Test
    void testCorsIntegration() throws Exception {
        // Test CORS integration with authentication
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", "https://trusted-app.com");
        headers.set("Access-Control-Request-Method", "GET");
        headers.set("Access-Control-Request-Headers", "Authorization");
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/fhir/Patient", 
            HttpMethod.OPTIONS, 
            entity, 
            String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getHeaders().get("Access-Control-Allow-Origin"));
        assertNotNull(response.getHeaders().get("Access-Control-Allow-Methods"));
        assertNotNull(response.getHeaders().get("Access-Control-Allow-Headers"));
    }

    // Helper methods for token creation

    private String createValidToken(String providerName) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("integration-test-user")
                .issuer("https://integration-test.com")
                .audience("test-audience")
                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .claim("resource_type", "Practitioner")
                .claim("fhir_id", "Practitioner/integration-test")
                .claim("roles", Arrays.asList("practitioner"))
                .claim("provider", providerName)
                .build();
        
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(rsaKey.getKeyID())
                        .build(),
                claimsSet);
        
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    private String createValidAzureToken() throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("azure-user-123")
                .issuer("https://sts.windows.net/tenant-id/")
                .audience("azure-audience")
                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .claim("tid", "azure-tenant-id")
                .claim("appid", "azure-app-id")
                .claim("resource_type", "Practitioner")
                .claim("fhir_id", "Practitioner/azure-user")
                .claim("roles", Arrays.asList("practitioner"))
                .build();
        
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(rsaKey.getKeyID())
                        .build(),
                claimsSet);
        
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    private String createExpiredToken() throws Exception {
        Instant past = Instant.now().minus(2, ChronoUnit.HOURS);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("expired-user")
                .issuer("https://test.com")
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

    private String createTokenWithRole(String resourceType, String role) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("role-test-user")
                .issuer("https://test.com")
                .audience("test-audience")
                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .claim("resource_type", resourceType)
                .claim("fhir_id", resourceType + "/role-test")
                .claim("roles", Arrays.asList(role))
                .build();
        
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(rsaKey.getKeyID())
                        .build(),
                claimsSet);
        
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    private String createMalformedToken() {
        return "not.a.valid.jwt.structure.at.all";
    }

    private String createTokenWithInvalidSignature() throws Exception {
        // Create a token but don't sign it properly
        Instant now = Instant.now();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("invalid-sig-user")
                .issuer("https://test.com")
                .audience("test-audience")
                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .build();
        
        // Create unsigned JWT and manually corrupt the signature
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(rsaKey.getKeyID())
                        .build(),
                claimsSet);
        
        // Return without signing or with corrupted signature
        return signedJWT.serialize().replaceAll("\\.[^.]*$", ".invalid_signature");
    }

    private String createJwtBombToken() throws Exception {
        // Create a token with deeply nested or large payload to test against JWT bomb attacks
        Instant now = Instant.now();
        
        // Create deeply nested structure
        Map<String, Object> nestedData = new HashMap<>();
        Map<String, Object> current = nestedData;
        for (int i = 0; i < 100; i++) {
            Map<String, Object> next = new HashMap<>();
            current.put("level" + i, next);
            current = next;
        }
        
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("bomb-test-user")
                .issuer("https://test.com")
                .audience("test-audience")
                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .claim("malicious_data", nestedData)
                .build();
        
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(rsaKey.getKeyID())
                        .build(),
                claimsSet);
        
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    private String createTokenWithNoneAlgorithm() throws Exception {
        // Create a token with "none" algorithm to test algorithm confusion
        Instant now = Instant.now();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("none-alg-user")
                .issuer("https://test.com")
                .audience("test-audience")
                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .build();
        
        // Create header with "none" algorithm
        JWSHeader noneHeader = new JWSHeader.Builder(new JWSAlgorithm("none"))
                .build();
        
        SignedJWT unsignedJWT = new SignedJWT(noneHeader, claimsSet);
        // Don't sign it - "none" algorithm means no signature
        return unsignedJWT.serialize();
    }
}
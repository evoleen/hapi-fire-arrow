package ca.uhn.fhir.jpa.starter.security;

import com.evoleen.hapi.faserver.auth.AuthConfigurationProperties;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for TokenClaimExtractor.
 * Tests claim extraction with various JWT claim configurations.
 */
@ExtendWith(MockitoExtension.class)
class TokenClaimExtractorTest {

    private TokenClaimExtractor tokenClaimExtractor;
    private AuthConfigurationProperties authConfig;
    
    @BeforeEach
    void setUp() {
        authConfig = createTestAuthConfiguration();
        tokenClaimExtractor = new TokenClaimExtractor(authConfig);
    }
    
    @Test
    void extractUserIdentity_ValidClaims_ReturnsUserIdentity() throws Exception {
        // Arrange
        JWTClaimsSet claimsSet = createValidClaimsSet();
        String providerName = "test-provider";
        
        // Act
        UserIdentity userIdentity = tokenClaimExtractor.extractUserIdentity(claimsSet, providerName);
        
        // Assert
        assertNotNull(userIdentity);
        assertEquals("test-user-123", userIdentity.getUserId());
        assertEquals("Practitioner", userIdentity.getFhirUserRoleResourceType());
        assertEquals("Practitioner/123", userIdentity.getFhirId());
        assertEquals("test@example.com", userIdentity.getEmail());
        assertEquals("Dr. Test User", userIdentity.getName());
        assertTrue(userIdentity.hasRole("practitioner"));
        assertTrue(userIdentity.hasRole("admin"));
    }
    
    @Test
    void extractUserIdentity_NullClaimsSet_ReturnsNull() {
        // Arrange
        JWTClaimsSet claimsSet = null;
        String providerName = "test-provider";
        
        // Act
        UserIdentity userIdentity = tokenClaimExtractor.extractUserIdentity(claimsSet, providerName);
        
        // Assert
        assertNull(userIdentity);
    }
    
    @Test
    void extractUserIdentity_MissingUserId_ReturnsNull() throws Exception {
        // Arrange
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim("resource_type", "Practitioner")
                .claim("fhir_id", "Practitioner/123")
                .build();
        String providerName = "test-provider";
        
        // Act
        UserIdentity userIdentity = tokenClaimExtractor.extractUserIdentity(claimsSet, providerName);
        
        // Assert
        assertNull(userIdentity);
    }
    
    @Test
    void extractUserIdentity_EmptyUserId_ReturnsNull() throws Exception {
        // Arrange
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("") // Empty user ID
                .claim("resource_type", "Practitioner")
                .build();
        String providerName = "test-provider";
        
        // Act
        UserIdentity userIdentity = tokenClaimExtractor.extractUserIdentity(claimsSet, providerName);
        
        // Assert
        assertNull(userIdentity);
    }
    
    @Test
    void extractUserIdentity_MinimalClaims_ReturnsUserIdentity() throws Exception {
        // Arrange
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("minimal-user")
                .build();
        String providerName = "test-provider";
        
        // Act
        UserIdentity userIdentity = tokenClaimExtractor.extractUserIdentity(claimsSet, providerName);
        
        // Assert
        assertNotNull(userIdentity);
        assertEquals("minimal-user", userIdentity.getUserId());
        assertNull(userIdentity.getFhirUserRoleResourceType());
        assertNull(userIdentity.getFhirId());
        assertNull(userIdentity.getEmail());
        assertNull(userIdentity.getName());
        assertTrue(userIdentity.getRoles().isEmpty());
    }
    
    @Test
    void extractUserIdentity_RolesAsString_ExtractsRoles() throws Exception {
        // Arrange
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("test-user")
                .claim("roles", "practitioner") // Single role as string
                .build();
        String providerName = "test-provider";
        
        // Act
        UserIdentity userIdentity = tokenClaimExtractor.extractUserIdentity(claimsSet, providerName);
        
        // Assert
        assertNotNull(userIdentity);
        assertEquals(1, userIdentity.getRoles().size());
        assertTrue(userIdentity.hasRole("practitioner"));
    }
    
    @Test
    void extractUserIdentity_RolesAsCommaSeparatedString_ExtractsRoles() throws Exception {
        // Arrange
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("test-user")
                .claim("roles", "practitioner,admin,user") // Multiple roles as comma-separated string
                .build();
        String providerName = "test-provider";
        
        // Act
        UserIdentity userIdentity = tokenClaimExtractor.extractUserIdentity(claimsSet, providerName);
        
        // Assert
        assertNotNull(userIdentity);
        assertEquals(3, userIdentity.getRoles().size());
        assertTrue(userIdentity.hasRole("practitioner"));
        assertTrue(userIdentity.hasRole("admin"));
        assertTrue(userIdentity.hasRole("user"));
    }
    
    @Test
    void extractUserIdentity_RolesAsArray_ExtractsRoles() throws Exception {
        // Arrange
        List<String> rolesList = Arrays.asList("practitioner", "admin", "user");
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("test-user")
                .claim("roles", rolesList) // Roles as array
                .build();
        String providerName = "test-provider";
        
        // Act
        UserIdentity userIdentity = tokenClaimExtractor.extractUserIdentity(claimsSet, providerName);
        
        // Assert
        assertNotNull(userIdentity);
        assertEquals(3, userIdentity.getRoles().size());
        assertTrue(userIdentity.hasRole("practitioner"));
        assertTrue(userIdentity.hasRole("admin"));
        assertTrue(userIdentity.hasRole("user"));
    }
    
    @Test
    void extractUserIdentity_ProviderSpecificClaimMapping_UsesProviderMapping() throws Exception {
        // Arrange
        // Create provider with custom claim mapping
        com.evoleen.hapi.faserver.auth.AuthProviderConfig.ClaimMapping customMapping = 
                new com.evoleen.hapi.faserver.auth.AuthProviderConfig.ClaimMapping();
        customMapping.setUserId("custom_user_id");
        customMapping.setUserRoleResourceType("custom_resource_type");
        customMapping.setFhirId("custom_fhir_id");
        
        authConfig.getProviders().get("test-provider").setClaimMapping(customMapping);
        
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim("custom_user_id", "custom-user-123")
                .claim("custom_resource_type", "Patient")
                .claim("custom_fhir_id", "Patient/456")
                .build();
        String providerName = "test-provider";
        
        // Act
        UserIdentity userIdentity = tokenClaimExtractor.extractUserIdentity(claimsSet, providerName);
        
        // Assert
        assertNotNull(userIdentity);
        assertEquals("custom-user-123", userIdentity.getUserId());
        assertEquals("Patient", userIdentity.getFhirUserRoleResourceType());
        assertEquals("Patient/456", userIdentity.getFhirId());
    }
    
    @Test
    void validateFhirClaims_ValidUserIdentity_ReturnsTrue() {
        // Arrange
        UserIdentity userIdentity = new UserIdentity.Builder()
                .userId("test-user")
                .fhirUserRoleResourceType("Practitioner")
                .fhirId("Practitioner/123")
                .build();
        
        // Act
        boolean isValid = tokenClaimExtractor.validateFhirClaims(userIdentity);
        
        // Assert
        assertTrue(isValid);
    }
    
    @Test
    void validateFhirClaims_NullUserIdentity_ReturnsFalse() {
        // Arrange
        UserIdentity userIdentity = null;
        
        // Act
        boolean isValid = tokenClaimExtractor.validateFhirClaims(userIdentity);
        
        // Assert
        assertFalse(isValid);
    }
    
    @Test
    void validateFhirClaims_MissingUserId_ReturnsFalse() {
        // Arrange
        UserIdentity userIdentity = new UserIdentity.Builder()
                .fhirUserRoleResourceType("Practitioner")
                .build();
        
        // Act
        boolean isValid = tokenClaimExtractor.validateFhirClaims(userIdentity);
        
        // Assert
        assertFalse(isValid);
    }
    
    @Test
    void validateFhirClaims_MissingFhirResourceType_ReturnsTrue() {
        // Arrange - FHIR resource type is optional per requirements
        UserIdentity userIdentity = new UserIdentity.Builder()
                .userId("test-user")
                .build();
        
        // Act
        boolean isValid = tokenClaimExtractor.validateFhirClaims(userIdentity);
        
        // Assert
        assertTrue(isValid); // Should be valid even without FHIR resource type
    }
    
    @Test
    void extractAllClaims_ValidClaimsSet_ReturnsAllClaims() throws Exception {
        // Arrange
        JWTClaimsSet claimsSet = createValidClaimsSet();
        
        // Act
        Map<String, Object> allClaims = tokenClaimExtractor.extractAllClaims(claimsSet);
        
        // Assert
        assertNotNull(allClaims);
        assertFalse(allClaims.isEmpty());
        assertEquals("test-user-123", allClaims.get("sub"));
        assertEquals("Practitioner", allClaims.get("resource_type"));
        assertEquals("Practitioner/123", allClaims.get("fhir_id"));
    }
    
    @Test
    void extractAllClaims_NullClaimsSet_ReturnsEmptyMap() {
        // Arrange
        JWTClaimsSet claimsSet = null;
        
        // Act
        Map<String, Object> allClaims = tokenClaimExtractor.extractAllClaims(claimsSet);
        
        // Assert
        assertNotNull(allClaims);
        assertTrue(allClaims.isEmpty());
    }
    
    @Test
    void hasClaim_ExistingClaim_ReturnsTrue() throws Exception {
        // Arrange
        JWTClaimsSet claimsSet = createValidClaimsSet();
        
        // Act & Assert
        assertTrue(tokenClaimExtractor.hasClaim(claimsSet, "sub"));
        assertTrue(tokenClaimExtractor.hasClaim(claimsSet, "resource_type"));
        assertTrue(tokenClaimExtractor.hasClaim(claimsSet, "email"));
    }
    
    @Test
    void hasClaim_NonExistingClaim_ReturnsFalse() throws Exception {
        // Arrange
        JWTClaimsSet claimsSet = createValidClaimsSet();
        
        // Act & Assert
        assertFalse(tokenClaimExtractor.hasClaim(claimsSet, "non_existing_claim"));
    }
    
    @Test
    void hasClaim_NullClaimsSet_ReturnsFalse() {
        // Arrange
        JWTClaimsSet claimsSet = null;
        
        // Act & Assert
        assertFalse(tokenClaimExtractor.hasClaim(claimsSet, "sub"));
    }
    
    @Test
    void hasClaim_NullClaimName_ReturnsFalse() throws Exception {
        // Arrange
        JWTClaimsSet claimsSet = createValidClaimsSet();
        
        // Act & Assert
        assertFalse(tokenClaimExtractor.hasClaim(claimsSet, null));
        assertFalse(tokenClaimExtractor.hasClaim(claimsSet, ""));
    }
    
    private AuthConfigurationProperties createTestAuthConfiguration() {
        AuthConfigurationProperties config = new AuthConfigurationProperties();
        config.setEnabled(true);
        
        // Create a test provider
        com.evoleen.hapi.faserver.auth.AuthConfigurationProperties.AuthProviderConfiguration provider = 
                new com.evoleen.hapi.faserver.auth.AuthConfigurationProperties.AuthProviderConfiguration();
        provider.setType("standard");
        provider.setEnabled(true);
        
        Map<String, com.evoleen.hapi.faserver.auth.AuthConfigurationProperties.AuthProviderConfiguration> providers = new HashMap<>();
        providers.put("test-provider", provider);
        config.setProviders(providers);
        
        // Set up default claim mapping
        com.evoleen.hapi.faserver.auth.AuthProviderConfig.ClaimMapping claimMapping = 
                new com.evoleen.hapi.faserver.auth.AuthProviderConfig.ClaimMapping();
        claimMapping.setUserId("sub");
        claimMapping.setUserRoleResourceType("resource_type");
        claimMapping.setFhirId("fhir_id");
        claimMapping.setRoles("roles");
        claimMapping.setEmail("email");
        claimMapping.setName("name");
        config.setDefaultClaimMapping(claimMapping);
        
        return config;
    }
    
    private JWTClaimsSet createValidClaimsSet() throws Exception {
        Instant now = Instant.now();
        List<String> roles = Arrays.asList("practitioner", "admin");
        
        return new JWTClaimsSet.Builder()
                .subject("test-user-123")
                .issuer("https://example.com")
                .audience("test-audience")
                .expirationTime(Date.from(now.plus(1, ChronoUnit.HOURS)))
                .issueTime(Date.from(now))
                .claim("resource_type", "Practitioner")
                .claim("fhir_id", "Practitioner/123")
                .claim("roles", roles)
                .claim("email", "test@example.com")
                .claim("name", "Dr. Test User")
                .build();
    }
}

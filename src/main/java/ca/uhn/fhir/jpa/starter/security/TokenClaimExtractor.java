package ca.uhn.fhir.jpa.starter.security;

import com.evoleen.hapi.faserver.auth.AuthConfigurationProperties;
import com.nimbusds.jwt.JWTClaimsSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.util.*;

/**
 * Token Claim Extractor that supports configurable claim fields for extracting
 * user identity information from JWT tokens, including FHIR-specific claims.
 * 
 * Supports configurable extraction of:
 * - FHIR user role resource type (e.g., "Practitioner", "Patient")
 * - FHIR ID (optional)
 * - User ID, roles, email, name
 */
@Component
@ConditionalOnProperty(prefix = "hapi.fhir.auth", name = "enabled", havingValue = "true")
public class TokenClaimExtractor {

    private static final Logger logger = LoggerFactory.getLogger(TokenClaimExtractor.class);
    
    private final AuthConfigurationProperties authConfig;
    
    public TokenClaimExtractor(AuthConfigurationProperties authConfig) {
        this.authConfig = authConfig;
    }
    
    /**
     * Extracts user identity information from JWT claims
     * 
     * @param claimsSet JWT claims set
     * @param providerName name of the OAuth provider
     * @return extracted user identity information
     */
    public UserIdentity extractUserIdentity(JWTClaimsSet claimsSet, String providerName) {
        if (claimsSet == null) {
            logger.warn("Claims set is null, cannot extract user identity");
            return null;
        }
        
        // Get claim mapping configuration for the provider
        com.evoleen.hapi.faserver.auth.AuthProviderConfig.ClaimMapping claimMapping = getClaimMapping(providerName);
        
        try {
            UserIdentity.Builder builder = new UserIdentity.Builder();
            
            // Extract user ID (required)
            String userId = extractStringClaim(claimsSet, claimMapping.getUserId());
            if (!StringUtils.hasText(userId)) {
                logger.warn("User ID claim '{}' is missing or empty", claimMapping.getUserId());
                return null;
            }
            builder.userId(userId);
            
            // Extract FHIR user role resource type (required for FHIR operations)
            String fhirResourceType = extractStringClaim(claimsSet, claimMapping.getUserRoleResourceType());
            if (StringUtils.hasText(fhirResourceType)) {
                builder.fhirUserRoleResourceType(fhirResourceType);
            }
            
            // Extract FHIR ID (optional)
            String fhirId = extractStringClaim(claimsSet, claimMapping.getFhirId());
            if (StringUtils.hasText(fhirId)) {
                builder.fhirId(fhirId);
            }
            
            // Extract user roles
            Set<String> roles = extractRoles(claimsSet, claimMapping.getRoles());
            builder.roles(roles);
            
            // Extract email
            String email = extractStringClaim(claimsSet, claimMapping.getEmail());
            if (StringUtils.hasText(email)) {
                builder.email(email);
            }
            
            // Extract name
            String name = extractStringClaim(claimsSet, claimMapping.getName());
            if (StringUtils.hasText(name)) {
                builder.name(name);
            }
            
            // Extract additional claims for debugging/logging
            builder.issuer(claimsSet.getIssuer());
            builder.subject(claimsSet.getSubject());
            builder.audience(claimsSet.getAudience());
            builder.expirationTime(claimsSet.getExpirationTime());
            builder.issuedAt(claimsSet.getIssueTime());
            
            UserIdentity identity = builder.build();
            
            logger.debug("Extracted user identity: userId={}, fhirResourceType={}, fhirId={}, roles={}", 
                    identity.getUserId(), identity.getFhirUserRoleResourceType(), 
                    identity.getFhirId(), identity.getRoles());
            
            return identity;
            
        } catch (Exception e) {
            logger.error("Error extracting user identity from JWT claims: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Gets claim mapping configuration for a provider
     */
    private com.evoleen.hapi.faserver.auth.AuthProviderConfig.ClaimMapping getClaimMapping(String providerName) {
        com.evoleen.hapi.faserver.auth.AuthConfigurationProperties.AuthProviderConfiguration provider = authConfig.getProviders().get(providerName);
        
        if (provider != null && provider.getClaimMapping() != null) {
            // Use provider-specific claim mapping
            return provider.getClaimMapping();
        }
        
        // Fall back to default claim mapping
        return authConfig.getDefaultClaimMapping();
    }
    
    /**
     * Extracts a string claim from JWT claims set
     */
    private String extractStringClaim(JWTClaimsSet claimsSet, String claimName) {
        if (!StringUtils.hasText(claimName)) {
            return null;
        }
        
        try {
            Object claimValue = claimsSet.getClaim(claimName);
            if (claimValue == null) {
                return null;
            }
            
            if (claimValue instanceof String) {
                return (String) claimValue;
            }
            
            // Convert to string if not already a string
            return claimValue.toString();
            
        } catch (Exception e) {
            logger.warn("Error extracting claim '{}': {}", claimName, e.getMessage());
            return null;
        }
    }
    
    /**
     * Extracts roles from JWT claims - supports both string and array formats
     */
    private Set<String> extractRoles(JWTClaimsSet claimsSet, String rolesClaimName) {
        Set<String> roles = new HashSet<>();
        
        if (!StringUtils.hasText(rolesClaimName)) {
            return roles;
        }
        
        try {
            Object rolesValue = claimsSet.getClaim(rolesClaimName);
            if (rolesValue == null) {
                return roles;
            }
            
            if (rolesValue instanceof String) {
                // Single role as string
                String role = ((String) rolesValue).trim();
                if (StringUtils.hasText(role)) {
                    // Check if it's a comma-separated list
                    if (role.contains(",")) {
                        Arrays.stream(role.split(","))
                                .map(String::trim)
                                .filter(StringUtils::hasText)
                                .forEach(roles::add);
                    } else {
                        roles.add(role);
                    }
                }
            } else if (rolesValue instanceof List) {
                // Multiple roles as array/list
                @SuppressWarnings("unchecked")
                List<Object> rolesList = (List<Object>) rolesValue;
                for (Object roleObj : rolesList) {
                    if (roleObj != null) {
                        String role = roleObj.toString().trim();
                        if (StringUtils.hasText(role)) {
                            roles.add(role);
                        }
                    }
                }
            } else {
                // Try to convert to string and parse
                String rolesStr = rolesValue.toString().trim();
                if (StringUtils.hasText(rolesStr)) {
                    if (rolesStr.contains(",")) {
                        Arrays.stream(rolesStr.split(","))
                                .map(String::trim)
                                .filter(StringUtils::hasText)
                                .forEach(roles::add);
                    } else {
                        roles.add(rolesStr);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("Error extracting roles from claim '{}': {}", rolesClaimName, e.getMessage());
        }
        
        return roles;
    }
    
    /**
     * Validates that required FHIR claims are present
     */
    public boolean validateFhirClaims(UserIdentity userIdentity) {
        if (userIdentity == null) {
            return false;
        }
        
        // User ID is always required
        if (!StringUtils.hasText(userIdentity.getUserId())) {
            logger.warn("User ID is missing from token claims");
            return false;
        }
        
        // FHIR resource type is highly recommended for proper FHIR operations
        if (!StringUtils.hasText(userIdentity.getFhirUserRoleResourceType())) {
            logger.warn("FHIR user role resource type is missing from token claims. " +
                    "This may limit FHIR operation capabilities.");
            // Don't fail validation as FHIR ID is optional per requirements
        }
        
        return true;
    }
    
    /**
     * Extracts all available claims for debugging purposes
     */
    public Map<String, Object> extractAllClaims(JWTClaimsSet claimsSet) {
        if (claimsSet == null) {
            return Collections.emptyMap();
        }
        
        try {
            return new HashMap<>(claimsSet.getClaims());
        } catch (Exception e) {
            logger.warn("Error extracting all claims: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
    
    /**
     * Checks if a claim exists in the claims set
     */
    public boolean hasClaim(JWTClaimsSet claimsSet, String claimName) {
        if (claimsSet == null || !StringUtils.hasText(claimName)) {
            return false;
        }
        
        try {
            return claimsSet.getClaim(claimName) != null;
        } catch (Exception e) {
            logger.warn("Error checking claim '{}': {}", claimName, e.getMessage());
            return false;
        }
    }
}

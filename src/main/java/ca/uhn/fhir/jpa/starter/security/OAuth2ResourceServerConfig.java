package ca.uhn.fhir.jpa.starter.security;

import ca.uhn.fhir.jpa.starter.config.AuthConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OAuth 2.0 Resource Server configuration for JWT token validation.
 * Supports multiple OAuth providers including Azure Identity and standard OIDC.
 */
@Component
@ConditionalOnProperty(name = "hapi.fhir.auth.enabled", havingValue = "true")
public class OAuth2ResourceServerConfig {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2ResourceServerConfig.class);

    private final AuthConfigurationProperties authConfig;

    public OAuth2ResourceServerConfig(AuthConfigurationProperties authConfig) {
        this.authConfig = authConfig;
    }

    /**
     * Creates a JWT decoder that supports multiple OAuth providers.
     * Configures JWT validation for Azure AD and standard OIDC providers.
     */
    public JwtDecoder jwtDecoder() {
        logger.info("Configuring JWT decoder for {} OAuth providers", authConfig.getProviders().size());

        if (authConfig.getProviders().isEmpty()) {
            throw new IllegalStateException("No OAuth providers configured. At least one provider must be enabled.");
        }

        // If only one provider, create a simple JWT decoder
        if (authConfig.getProviders().size() == 1) {
            Map.Entry<String, AuthConfigurationProperties.OAuthProvider> entry = 
                authConfig.getProviders().entrySet().iterator().next();
            return createJwtDecoderForProvider(entry.getKey(), entry.getValue());
        }

        // Multiple providers - create a delegating decoder
        return createMultiProviderJwtDecoder();
    }

    /**
     * Creates a JWT authentication converter that extracts user authorities and identity
     * from JWT claims based on the configured claim mappings.
     */
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        logger.info("Configuring JWT authentication converter with claim mappings");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        
        // Set up authorities converter to extract roles from JWT claims
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        
        // Set principal name converter to extract user identity
        converter.setPrincipalClaimName(authConfig.getDefaultClaimMapping().getUserId());
        
        return converter;
    }

    /**
     * Creates a JWT granted authorities converter that extracts roles and permissions
     * from JWT claims.
     */
    private Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return jwt -> {
            // Get the appropriate claim mapping (provider-specific or default)
            AuthConfigurationProperties.ClaimMapping claimMapping = authConfig.getDefaultClaimMapping();
            
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            
            // Extract roles from JWT claims
            Object rolesClaimValue = jwt.getClaim(claimMapping.getRoles());
            if (rolesClaimValue != null) {
                Collection<String> roles = extractRoles(rolesClaimValue);
                authorities.addAll(roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList()));
            }
            
            // Extract FHIR resource type role if present
            String resourceType = jwt.getClaimAsString(claimMapping.getUserRoleResourceType());
            if (resourceType != null) {
                authorities.add(new SimpleGrantedAuthority("FHIR_" + resourceType.toUpperCase()));
            }
            
            // Add default authenticated authority
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            
            logger.debug("Extracted authorities for user {}: {}", 
                jwt.getClaimAsString(claimMapping.getUserId()), authorities);
            
            return authorities;
        };
    }

    /**
     * Creates a JWT decoder for a single OAuth provider.
     */
    private JwtDecoder createJwtDecoderForProvider(String providerName, AuthConfigurationProperties.OAuthProvider provider) {
        logger.info("Creating JWT decoder for provider: {} (type: {})", providerName, provider.getType());

        try {
            if ("azure".equalsIgnoreCase(provider.getType())) {
                return createAzureJwtDecoder(provider.getAzure());
            } else if ("standard".equalsIgnoreCase(provider.getType())) {
                return createStandardJwtDecoder(provider.getStandard());
            } else {
                throw new IllegalArgumentException("Unsupported OAuth provider type: " + provider.getType());
            }
        } catch (Exception e) {
            logger.error("Failed to create JWT decoder for provider {}: {}", providerName, e.getMessage());
            throw new IllegalStateException("Cannot configure JWT decoder for provider: " + providerName, e);
        }
    }

    /**
     * Creates a JWT decoder for Azure Active Directory.
     */
    private JwtDecoder createAzureJwtDecoder(AuthConfigurationProperties.AzureConfig azureConfig) {
        String issuerUri = azureConfig.getInstance() + azureConfig.getTenantId() + "/v2.0";
        logger.info("Creating Azure JWT decoder with issuer: {}", issuerUri);
        
        return NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
    }

    /**
     * Creates a JWT decoder for standard OAuth/OIDC providers.
     */
    private JwtDecoder createStandardJwtDecoder(AuthConfigurationProperties.StandardOAuthConfig standardConfig) {
        logger.info("Creating standard OIDC JWT decoder with discovery URL: {}", standardConfig.getDiscoveryUrl());
        
        return NimbusJwtDecoder.withIssuerLocation(standardConfig.getDiscoveryUrl()).build();
    }

    /**
     * Creates a multi-provider JWT decoder that can handle tokens from multiple OAuth providers.
     */
    private JwtDecoder createMultiProviderJwtDecoder() {
        logger.info("Creating multi-provider JWT decoder");
        
        Map<String, JwtDecoder> decoders = new HashMap<>();
        
        for (Map.Entry<String, AuthConfigurationProperties.OAuthProvider> entry : authConfig.getProviders().entrySet()) {
            if (entry.getValue().isEnabled()) {
                decoders.put(entry.getKey(), createJwtDecoderForProvider(entry.getKey(), entry.getValue()));
            }
        }
        
        return new MultiProviderJwtDecoder(decoders);
    }

    /**
     * Extracts roles from a claim value, handling both string and array formats.
     */
    private Collection<String> extractRoles(Object rolesClaimValue) {
        if (rolesClaimValue instanceof String) {
            return Arrays.asList(((String) rolesClaimValue).split(","));
        } else if (rolesClaimValue instanceof Collection) {
            return ((Collection<?>) rolesClaimValue).stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        } else if (rolesClaimValue instanceof String[]) {
            return Arrays.asList((String[]) rolesClaimValue);
        }
        return Collections.emptyList();
    }

    /**
     * Multi-provider JWT decoder that delegates to the appropriate provider-specific decoder.
     */
    private static class MultiProviderJwtDecoder implements JwtDecoder {
        private static final Logger logger = LoggerFactory.getLogger(MultiProviderJwtDecoder.class);
        
        private final Map<String, JwtDecoder> decoders;
        
        public MultiProviderJwtDecoder(Map<String, JwtDecoder> decoders) {
            this.decoders = decoders;
        }
        
        @Override
        public Jwt decode(String token) throws JwtException {
            JwtException lastException = null;
            
            // Try each decoder until one succeeds
            for (Map.Entry<String, JwtDecoder> entry : decoders.entrySet()) {
                try {
                    Jwt jwt = entry.getValue().decode(token);
                    logger.debug("Successfully decoded JWT using provider: {}", entry.getKey());
                    return jwt;
                } catch (JwtException e) {
                    logger.debug("Failed to decode JWT with provider {}: {}", entry.getKey(), e.getMessage());
                    lastException = e;
                }
            }
            
            logger.error("Failed to decode JWT with any configured provider");
            throw new JwtException("Unable to decode JWT with any configured provider", lastException);
        }
    }
}

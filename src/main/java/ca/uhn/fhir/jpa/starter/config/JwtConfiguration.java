package ca.uhn.fhir.jpa.starter.config;

import ca.uhn.fhir.jpa.starter.security.JwtAccessDeniedHandler;
import ca.uhn.fhir.jpa.starter.security.JwtAuthenticationEntryPoint;
import ca.uhn.fhir.jpa.starter.security.JwtTokenValidator;
import ca.uhn.fhir.jpa.starter.security.TokenClaimExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.concurrent.TimeUnit;

/**
 * JWT Configuration class that configures Spring Security OAuth2 Resource Server
 * for JWT token validation with support for multiple OAuth providers including
 * Azure Identity and standard OAuth/OIDC.
 * 
 * Features:
 * - Configurable JWT validation algorithms (RS256, RS384, RS512)
 * - Support for Azure AD and standard OIDC providers
 * - High-performance caching for JWK sets and validation results
 * - Automatic cache cleanup scheduling
 * - Custom claim extraction for FHIR-specific claims
 */
@Configuration
@EnableWebSecurity
@EnableScheduling
@ConditionalOnProperty(prefix = "hapi.fhir.auth", name = "enabled", havingValue = "true")
public class JwtConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(JwtConfiguration.class);
    
    private final AuthConfigurationProperties authConfig;
    private final JwtTokenValidator jwtTokenValidator;
    private final TokenClaimExtractor tokenClaimExtractor;
    
    public JwtConfiguration(AuthConfigurationProperties authConfig,
                          JwtTokenValidator jwtTokenValidator,
                          TokenClaimExtractor tokenClaimExtractor) {
        this.authConfig = authConfig;
        this.jwtTokenValidator = jwtTokenValidator;
        this.tokenClaimExtractor = tokenClaimExtractor;
    }
    
    /**
     * Configures the security filter chain for JWT authentication
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring JWT security filter chain with {} providers", 
                authConfig.getProviders().size());
        
        http
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .oauth2ResourceServer(oauth2 -> 
                oauth2.jwt(jwt -> 
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .authorizeHttpRequests(authz -> authz
                // Allow actuator health check without authentication
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                // Allow FHIR metadata endpoint without authentication
                .requestMatchers("/fhir/metadata").permitAll()
                // Require authentication for all other FHIR endpoints
                .requestMatchers("/fhir/**").authenticated()
                // Allow all other requests (web UI, etc.)
                .anyRequest().permitAll())
            .cors(cors -> cors.disable()) // CORS will be handled by HAPI FHIR
            .csrf(csrf -> csrf.disable()); // CSRF not needed for REST APIs
        
        return http.build();
    }
    
    /**
     * Custom JWT authentication converter that extracts user identity from JWT claims
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        
        // Set custom authorities converter to extract roles from JWT
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // This will be implemented to convert JWT claims to Spring Security authorities
            // For now, return empty collection as the actual authorization will be handled
            // by HAPI FHIR interceptors using the extracted UserIdentity
            return java.util.Collections.emptyList();
        });
        
        // Set principal name to use the user ID claim
        converter.setPrincipalClaimName(authConfig.getDefaultClaimMapping().getUserId());
        
        return converter;
    }
    
    /**
     * Scheduled task to clean up expired cache entries every 15 minutes
     */
    @Scheduled(fixedRate = 15, timeUnit = TimeUnit.MINUTES)
    public void cleanupExpiredCacheEntries() {
        try {
            logger.debug("Running scheduled cache cleanup");
            jwtTokenValidator.cleanupExpiredCacheEntries();
        } catch (Exception e) {
            logger.error("Error during scheduled cache cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Validates the JWT configuration on startup
     */
    @Bean
    public JwtConfigurationValidator jwtConfigurationValidator() {
        return new JwtConfigurationValidator(authConfig);
    }
    
    /**
     * Configuration validator to ensure JWT setup is correct
     */
    public static class JwtConfigurationValidator {
        private final AuthConfigurationProperties authConfig;
        
        public JwtConfigurationValidator(AuthConfigurationProperties authConfig) {
            this.authConfig = authConfig;
            validateConfiguration();
        }
        
        private void validateConfiguration() {
            if (!authConfig.isEnabled()) {
                logger.info("JWT authentication is disabled");
                return;
            }
            
            if (authConfig.getProviders().isEmpty()) {
                logger.warn("JWT authentication is enabled but no providers are configured");
                return;
            }
            
            for (var entry : authConfig.getProviders().entrySet()) {
                String providerName = entry.getKey();
                var provider = entry.getValue();
                
                if (!provider.isEnabled()) {
                    logger.info("Provider '{}' is disabled", providerName);
                    continue;
                }
                
                validateProvider(providerName, provider);
            }
            
            validateClaimMapping("default", authConfig.getDefaultClaimMapping());
            
            logger.info("JWT configuration validation completed successfully");
        }
        
        private void validateProvider(String providerName, AuthConfigurationProperties.OAuthProvider provider) {
            String type = provider.getType();
            
            if ("azure".equalsIgnoreCase(type)) {
                validateAzureProvider(providerName, provider);
            } else if ("standard".equalsIgnoreCase(type)) {
                validateStandardProvider(providerName, provider);
            } else {
                logger.error("Provider '{}' has unsupported type '{}'. Supported types: azure, standard", 
                        providerName, type);
            }
            
            if (provider.getClaimMapping() != null) {
                validateClaimMapping(providerName, provider.getClaimMapping());
            }
        }
        
        private void validateAzureProvider(String providerName, AuthConfigurationProperties.OAuthProvider provider) {
            var azure = provider.getAzure();
            if (azure == null) {
                logger.error("Provider '{}' is type 'azure' but azure configuration is missing", providerName);
                return;
            }
            
            if (azure.getTenantId() == null || azure.getTenantId().trim().isEmpty()) {
                logger.error("Provider '{}' azure configuration is missing tenantId", providerName);
            }
            
            if (azure.getApplicationId() == null || azure.getApplicationId().trim().isEmpty()) {
                logger.error("Provider '{}' azure configuration is missing applicationId", providerName);
            }
            
            logger.info("Azure provider '{}' configured for tenant '{}'", providerName, azure.getTenantId());
        }
        
        private void validateStandardProvider(String providerName, AuthConfigurationProperties.OAuthProvider provider) {
            var standard = provider.getStandard();
            if (standard == null) {
                logger.error("Provider '{}' is type 'standard' but standard configuration is missing", providerName);
                return;
            }
            
            if (standard.getDiscoveryUrl() == null || standard.getDiscoveryUrl().trim().isEmpty()) {
                logger.error("Provider '{}' standard configuration is missing discoveryUrl", providerName);
            }
            
            if (standard.getAudience() == null || standard.getAudience().trim().isEmpty()) {
                logger.error("Provider '{}' standard configuration is missing audience", providerName);
            }
            
            logger.info("Standard OAuth provider '{}' configured with discovery URL '{}'", 
                    providerName, standard.getDiscoveryUrl());
        }
        
        private void validateClaimMapping(String context, AuthConfigurationProperties.ClaimMapping claimMapping) {
            if (claimMapping.getUserId() == null || claimMapping.getUserId().trim().isEmpty()) {
                logger.warn("{} claim mapping is missing userId field - this may cause authentication failures", context);
            }
            
            if (claimMapping.getUserRoleResourceType() == null || claimMapping.getUserRoleResourceType().trim().isEmpty()) {
                logger.warn("{} claim mapping is missing userRoleResourceType field - FHIR operations may be limited", context);
            }
            
            logger.debug("{} claim mapping: userId='{}', fhirResourceType='{}', fhirId='{}'", 
                    context, claimMapping.getUserId(), claimMapping.getUserRoleResourceType(), 
                    claimMapping.getFhirId());
        }
    }
}

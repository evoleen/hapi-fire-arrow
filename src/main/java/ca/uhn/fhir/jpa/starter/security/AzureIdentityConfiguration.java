package ca.uhn.fhir.jpa.starter.security;

import com.evoleen.hapi.faserver.auth.AuthConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Configuration for Azure Identity OAuth 2.0 integration.
 * Provides Azure-specific JWT decoder configuration and validation.
 */
@Configuration
@ConditionalOnProperty(name = "hapi.fhir.auth.enabled", havingValue = "true")
public class AzureIdentityConfiguration {

    private final AuthConfigurationProperties authProperties;

    public AzureIdentityConfiguration(AuthConfigurationProperties authProperties) {
        this.authProperties = authProperties;
    }

    /**
     * Creates a JWT decoder for Azure Active Directory tokens.
     * Configured based on Azure-specific provider configurations.
     * 
     * @return JwtDecoder configured for Azure AD
     */
    @Bean
    @ConditionalOnProperty(name = "hapi.fhir.auth.providers.azure_example.enabled", havingValue = "true")
    public JwtDecoder azureJwtDecoder() {
        // Get Azure configuration from the first Azure provider found
        com.evoleen.hapi.faserver.auth.AuthConfigurationProperties.AuthProviderConfiguration azureProvider = getAzureProvider();
        if (azureProvider == null || azureProvider.getAzure() == null) {
            throw new IllegalStateException("Azure OAuth provider configuration not found or incomplete");
        }

        AuthConfigurationProperties.AzureConfig azureConfig = azureProvider.getAzure();
        
        // Construct Azure AD OIDC discovery URL
        String discoveryUrl = azureConfig.getInstance() + azureConfig.getTenantId() + "/v2.0/.well-known/openid-configuration";
        
        // Create JWT decoder with Azure AD configuration
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(
            discoveryUrl.replace(".well-known/openid-configuration", "discovery/v2.0/keys")
        ).build();
        
        // Set up token validators
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(new JwtTimestampValidator());
        validators.add(new AzureAudienceValidator(azureConfig.getApplicationId()));
        validators.add(new JwtIssuerValidator(azureConfig.getInstance() + azureConfig.getTenantId() + "/v2.0"));
        
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        
        return jwtDecoder;
    }

    /**
     * Creates a JWT authentication converter that extracts authorities from Azure AD tokens.
     * Uses configurable claim mappings.
     * 
     * @return JwtAuthenticationConverter configured for Azure AD
     */
    @Bean
    @ConditionalOnProperty(name = "hapi.fhir.auth.providers.azure_example.enabled", havingValue = "true")
    public JwtAuthenticationConverter azureJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        
        // Set up authorities converter with configurable claim mapping
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        
        // Get claim mapping (provider-specific or default)
        com.evoleen.hapi.faserver.auth.AuthProviderConfig.ClaimMapping claimMapping = getEffectiveClaimMapping();
        authoritiesConverter.setAuthoritiesClaimName(claimMapping.getRoles());
        authoritiesConverter.setAuthorityPrefix(""); // No prefix for roles
        
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        
        return converter;
    }

    /**
     * Finds the first enabled Azure OAuth provider configuration.
     * 
     * @return Azure OAuth provider configuration or null if not found
     */
    private com.evoleen.hapi.faserver.auth.AuthConfigurationProperties.AuthProviderConfiguration getAzureProvider() {
        return authProperties.getProviders().values().stream()
            .filter(provider -> "azure".equals(provider.getType()) && provider.isEnabled())
            .findFirst()
            .orElse(null);
    }

    /**
     * Gets the effective claim mapping for the Azure provider.
     * Uses provider-specific mapping if available, otherwise falls back to defaults.
     * 
     * @return Effective claim mapping
     */
    private com.evoleen.hapi.faserver.auth.AuthProviderConfig.ClaimMapping getEffectiveClaimMapping() {
        com.evoleen.hapi.faserver.auth.AuthConfigurationProperties.AuthProviderConfiguration azureProvider = getAzureProvider();
        if (azureProvider != null && azureProvider.getClaimMapping() != null) {
            return azureProvider.getClaimMapping();
        }
        return authProperties.getDefaultClaimMapping();
    }

    /**
     * Custom audience validator for Azure AD tokens.
     * Validates that the token audience matches the configured application ID.
     */
    private static class AzureAudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final String expectedAudience;

        public AzureAudienceValidator(String expectedAudience) {
            this.expectedAudience = expectedAudience;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            Collection<String> audiences = jwt.getAudience();
            if (audiences != null && audiences.contains(expectedAudience)) {
                return OAuth2TokenValidatorResult.success();
            }
            
            OAuth2Error error = new OAuth2Error(
                "invalid_audience",
                "The token audience does not match the expected application ID",
                null
            );
            return OAuth2TokenValidatorResult.failure(error);
        }
    }
}
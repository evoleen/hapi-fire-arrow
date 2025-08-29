package com.evoleen.hapi.faserver.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for OAuth 2.0 authentication and authorization.
 * Uses AuthProvider pattern with concrete implementations for different OAuth providers.
 */
@ConfigurationProperties(prefix = "hapi.fhir.auth")
@Configuration
@EnableConfigurationProperties
@Validated
public class AuthConfigurationProperties {

    /**
     * Whether OAuth authentication is enabled
     */
    private boolean enabled = false;

    /**
     * Whether OAuth authentication is required for all requests
     * If false, requests without authentication may still be allowed
     */
    private boolean required = true;

    /**
     * Authentication provider configurations mapped by provider name.
     * Each provider has a type field that determines the concrete implementation:
     * - "oauth": Standard OAuth/OIDC provider
     * - "azure_identity": Azure Identity provider
     */
    @Valid
    private Map<String, AuthProviderConfiguration> providers = new HashMap<>();

    /**
     * Default claim field mappings for user identity extraction
     */
    @Valid
    private AuthProviderConfig.ClaimMapping defaultClaimMapping = new AuthProviderConfig.ClaimMapping();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public Map<String, AuthProviderConfiguration> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, AuthProviderConfiguration> providers) {
        this.providers = providers;
    }

    public AuthProviderConfig.ClaimMapping getDefaultClaimMapping() {
        return defaultClaimMapping;
    }

    public void setDefaultClaimMapping(AuthProviderConfig.ClaimMapping defaultClaimMapping) {
        this.defaultClaimMapping = defaultClaimMapping;
    }

    /**
     * Authentication provider configuration wrapper.
     * Contains type-specific configurations for OAuth and Azure Identity providers.
     */
    public static class AuthProviderConfiguration {

        /**
         * Type of authentication provider ("oauth", "azure_identity")
         */
        private String type;

        /**
         * Whether this provider is enabled
         */
        private boolean enabled = true;

        /**
         * OAuth provider configuration (when type = "oauth")
         */
        @Valid
        private OAuthProviderConfig oauth;

        /**
         * Azure Identity provider configuration (when type = "azure_identity")
         */
        @Valid
        private AzureIdentityProviderConfig azureIdentity;

        /**
         * Provider-specific claim mappings (overrides defaults)
         */
        @Valid
        private AuthProviderConfig.ClaimMapping claimMapping;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public OAuthProviderConfig getOauth() {
            return oauth;
        }

        public void setOauth(OAuthProviderConfig oauth) {
            this.oauth = oauth;
        }

        public AzureIdentityProviderConfig getAzureIdentity() {
            return azureIdentity;
        }

        public void setAzureIdentity(AzureIdentityProviderConfig azureIdentity) {
            this.azureIdentity = azureIdentity;
        }

        public AuthProviderConfig.ClaimMapping getClaimMapping() {
            return claimMapping;
        }

        public void setClaimMapping(AuthProviderConfig.ClaimMapping claimMapping) {
            this.claimMapping = claimMapping;
        }
    }
}

package ca.uhn.fhir.jpa.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for OAuth 2.0 authentication and authorization.
 * Supports multiple OAuth providers including Azure Identity and standard OAuth/OIDC.
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
     * OAuth provider configurations mapped by provider name
     */
    @Valid
    private Map<String, OAuthProvider> providers = new HashMap<>();

    /**
     * Default claim field mappings for user identity extraction
     */
    @Valid
    private ClaimMapping defaultClaimMapping = new ClaimMapping();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, OAuthProvider> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, OAuthProvider> providers) {
        this.providers = providers;
    }

    public ClaimMapping getDefaultClaimMapping() {
        return defaultClaimMapping;
    }

    public void setDefaultClaimMapping(ClaimMapping defaultClaimMapping) {
        this.defaultClaimMapping = defaultClaimMapping;
    }

    /**
     * OAuth provider configuration
     */
    public static class OAuthProvider {

        /**
         * Type of OAuth provider (azure, standard)
         */
        @NotEmpty
        private String type;

        /**
         * Whether this provider is enabled
         */
        private boolean enabled = true;

        /**
         * Azure Identity specific configuration
         */
        @Valid
        private AzureConfig azure;

        /**
         * Standard OAuth/OIDC configuration
         */
        @Valid
        private StandardOAuthConfig standard;

        /**
         * Provider-specific claim mappings (overrides defaults)
         */
        @Valid
        private ClaimMapping claimMapping;

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

        public AzureConfig getAzure() {
            return azure;
        }

        public void setAzure(AzureConfig azure) {
            this.azure = azure;
        }

        public StandardOAuthConfig getStandard() {
            return standard;
        }

        public void setStandard(StandardOAuthConfig standard) {
            this.standard = standard;
        }

        public ClaimMapping getClaimMapping() {
            return claimMapping;
        }

        public void setClaimMapping(ClaimMapping claimMapping) {
            this.claimMapping = claimMapping;
        }
    }

    /**
     * Azure Identity specific configuration
     */
    public static class AzureConfig {

        /**
         * Azure Active Directory tenant ID
         */
        @NotEmpty
        private String tenantId;

        /**
         * Azure application (client) ID
         */
        @NotEmpty
        private String applicationId;

        /**
         * Azure instance (defaults to https://login.microsoftonline.com/)
         */
        private String instance = "https://login.microsoftonline.com/";

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(String applicationId) {
            this.applicationId = applicationId;
        }

        public String getInstance() {
            return instance;
        }

        public void setInstance(String instance) {
            this.instance = instance;
        }
    }

    /**
     * Standard OAuth/OIDC configuration
     */
    public static class StandardOAuthConfig {

        /**
         * OIDC discovery URL for automatic configuration
         */
        @NotEmpty
        private String discoveryUrl;

        /**
         * Expected audience for JWT tokens
         */
        @NotEmpty
        private String audience;

        /**
         * Client ID (optional, for client credentials flow)
         */
        private String clientId;

        /**
         * Client secret (optional, for client credentials flow)
         */
        private String clientSecret;

        public String getDiscoveryUrl() {
            return discoveryUrl;
        }

        public void setDiscoveryUrl(String discoveryUrl) {
            this.discoveryUrl = discoveryUrl;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }
    }

    /**
     * JWT token claim field mappings for extracting user identity and role information
     */
    public static class ClaimMapping {

        /**
         * JWT claim field containing the FHIR user role resource type (e.g., "Practitioner", "Patient")
         */
        private String userRoleResourceType = "resource_type";

        /**
         * JWT claim field containing the FHIR ID (optional)
         */
        private String fhirId = "fhir_id";

        /**
         * JWT claim field containing the user identifier
         */
        private String userId = "sub";

        /**
         * JWT claim field containing user roles/permissions
         */
        private String roles = "roles";

        /**
         * JWT claim field containing user email
         */
        private String email = "email";

        /**
         * JWT claim field containing user name
         */
        private String name = "name";

        public String getUserRoleResourceType() {
            return userRoleResourceType;
        }

        public void setUserRoleResourceType(String userRoleResourceType) {
            this.userRoleResourceType = userRoleResourceType;
        }

        public String getFhirId() {
            return fhirId;
        }

        public void setFhirId(String fhirId) {
            this.fhirId = fhirId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getRoles() {
            return roles;
        }

        public void setRoles(String roles) {
            this.roles = roles;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}

package ca.uhn.fhir.jpa.starter.auth;

import jakarta.validation.Valid;

/**
 * Base configuration for authentication providers.
 */
public abstract class AuthProviderConfig {

    /**
     * Provider type identifier
     */
    private String type;

    /**
     * Whether this provider is enabled
     */
    private boolean enabled = true;

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

    public ClaimMapping getClaimMapping() {
        return claimMapping;
    }

    public void setClaimMapping(ClaimMapping claimMapping) {
        this.claimMapping = claimMapping;
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

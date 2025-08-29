package com.evoleen.hapi.faserver.auth;

import jakarta.validation.constraints.NotEmpty;

/**
 * Configuration for Azure Identity authentication provider.
 */
public class AzureIdentityProviderConfig extends AuthProviderConfig {

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

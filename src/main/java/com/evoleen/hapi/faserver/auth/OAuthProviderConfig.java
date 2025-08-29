package com.evoleen.hapi.faserver.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

/**
 * Configuration for standard OAuth/OIDC authentication provider.
 */
public class OAuthProviderConfig extends AuthProviderConfig {

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

package ca.uhn.fhir.jpa.starter.auth;

import ca.uhn.fhir.jpa.starter.security.TokenClaimExtractor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Factory for creating AuthProvider instances based on configuration.
 */
@Component
public class AuthProviderFactory {

    private final TokenClaimExtractor claimExtractor;

    public AuthProviderFactory(TokenClaimExtractor claimExtractor) {
        this.claimExtractor = claimExtractor;
    }

    /**
     * Creates a list of AuthProvider instances from configuration.
     *
     * @param config the authentication configuration
     * @return list of configured and enabled auth providers
     */
    public List<AuthProvider> createProviders(AuthConfigurationProperties config) {
        List<AuthProvider> providers = new ArrayList<>();

        for (Map.Entry<String, AuthConfigurationProperties.AuthProviderConfiguration> entry : 
             config.getProviders().entrySet()) {
            
            String providerName = entry.getKey();
            AuthConfigurationProperties.AuthProviderConfiguration providerConfig = entry.getValue();

            if (!providerConfig.isEnabled()) {
                continue;
            }

            AuthProvider provider = createProvider(providerName, providerConfig, config);
            if (provider != null) {
                providers.add(provider);
            }
        }

        return providers;
    }

    private AuthProvider createProvider(String name, 
                                      AuthConfigurationProperties.AuthProviderConfiguration providerConfig,
                                      AuthConfigurationProperties globalConfig) {
        
        switch (providerConfig.getType()) {
            case "oauth":
                if (providerConfig.getOauth() != null) {
                    // Use provider-specific claim mapping or fall back to global default
                    AuthProviderConfig.ClaimMapping claimMapping = providerConfig.getClaimMapping() != null 
                        ? providerConfig.getClaimMapping() 
                        : globalConfig.getDefaultClaimMapping();
                        
                    OAuthProviderConfig config = providerConfig.getOauth();
                    config.setClaimMapping(claimMapping);
                    config.setEnabled(providerConfig.isEnabled());
                    config.setType(providerConfig.getType());
                    
                    return new OAuthProvider(name, config, claimExtractor);
                }
                break;
                
            case "azure_identity":
                if (providerConfig.getAzureIdentity() != null) {
                    // Use provider-specific claim mapping or fall back to global default
                    AuthProviderConfig.ClaimMapping claimMapping = providerConfig.getClaimMapping() != null 
                        ? providerConfig.getClaimMapping() 
                        : globalConfig.getDefaultClaimMapping();
                        
                    AzureIdentityProviderConfig config = providerConfig.getAzureIdentity();
                    config.setClaimMapping(claimMapping);
                    config.setEnabled(providerConfig.isEnabled());
                    config.setType(providerConfig.getType());
                    
                    return new AzureIdentityProvider(name, config, claimExtractor);
                }
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported auth provider type: " + providerConfig.getType());
        }
        
        return null;
    }
}

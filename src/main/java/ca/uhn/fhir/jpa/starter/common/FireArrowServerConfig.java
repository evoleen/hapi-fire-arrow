package ca.uhn.fhir.jpa.starter.common;

import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.to.FhirTesterMvcConfig;
import ca.uhn.fhir.to.TesterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

// @formatter:off
/**
 * This spring config file configures the Fire Arrow Server web interface.
 * It serves two purposes:
 * 1. It imports FhirTesterMvcConfig, which provides the web UI framework
 * 2. It configures the interface to connect to Fire Arrow Server via the 
 *    fireArrowServerConfig() method below
 */
@Configuration
@Import(FhirTesterMvcConfig.class)
@Conditional(FireArrowServerConfigCondition.class)
public class FireArrowServerConfig {

	/**
	 * This bean configures the Fire Arrow Server web interface to communicate
	 * with the local FHIR server. The configuration is read from the 
	 * fire_arrow_server section in application.yaml.
	 * 
	 * The interface provides a branded web UI for interacting with the FHIR
	 * server, including browsing resources, testing endpoints, and viewing
	 * server capabilities.
	 */
	@Bean
	public TesterConfig testerConfig(AppProperties appProperties) {
		TesterConfig retVal = new TesterConfig();
		appProperties.getFire_arrow_server().forEach((key, value) -> {
			retVal.addServer()
					.withId(key)
					.withFhirVersion(value.getFhir_version())
					.withBaseUrl(value.getServer_address())
					.withName(value.getName());
			retVal.setRefuseToFetchThirdPartyUrls(value.getRefuse_to_fetch_third_party_urls());
		});
		return retVal;
	}
}
// @formatter:on

package ca.uhn.fhir.jpa.starter.common;

import ca.uhn.fhir.jpa.starter.util.EnvironmentHelper;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class FhirTesterConfigCondition implements Condition {
	@Override
	public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata metadata) {
		// Fire Arrow Server: Disable tester UI while keeping functionality available
		// The tester functionality is hidden from the UI but the server configuration
		// is now handled through fire_arrow_server settings in application.yaml
		return false;
	}
}

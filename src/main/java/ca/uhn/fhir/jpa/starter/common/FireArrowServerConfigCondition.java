package ca.uhn.fhir.jpa.starter.common;

import ca.uhn.fhir.jpa.starter.util.EnvironmentHelper;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class FireArrowServerConfigCondition implements Condition {
	@Override
	public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata metadata) {
		// Fire Arrow Server: Enable UI when fire_arrow_server configuration is present
		ConfigurableEnvironment configurableEnvironment = (ConfigurableEnvironment) conditionContext.getEnvironment();
		return EnvironmentHelper.isFireArrowServerUiEnabled(configurableEnvironment);
	}
}

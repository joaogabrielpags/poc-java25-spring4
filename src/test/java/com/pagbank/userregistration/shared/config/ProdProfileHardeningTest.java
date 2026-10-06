package com.pagbank.userregistration.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

/**
 * Confirma T5.3: hardening do perfil {@code prod} — sampling de tracing reduzido
 * (RNF-02), actuator restrito a {@code health,info} e H2 console desabilitado (D3).
 */
@SpringBootTest
@ActiveProfiles({"prod", "test"})
class ProdProfileHardeningTest {

	@Autowired
	private Environment environment;

	@Test
	void tracingSamplingIsReducedInProd() {
		assertThat(environment.getProperty("management.tracing.sampling.probability", Double.class))
				.isEqualTo(0.1);
	}

	@Test
	void actuatorExposureIsRestrictedInProd() {
		assertThat(environment.getProperty("management.endpoints.web.exposure.include"))
				.isEqualTo("health,info");
		assertThat(environment.getProperty("management.endpoint.health.show-details"))
				.isEqualTo("never");
	}

	@Test
	void h2ConsoleIsDisabledInProd() {
		assertThat(environment.getProperty("spring.h2.console.enabled", Boolean.class)).isFalse();
	}
}

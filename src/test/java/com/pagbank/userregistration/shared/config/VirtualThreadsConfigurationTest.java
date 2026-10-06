package com.pagbank.userregistration.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

/**
 * Confirma RNF-05: virtual threads habilitadas na aplicação (T1.7).
 */
@SpringBootTest
@ActiveProfiles("test")
class VirtualThreadsConfigurationTest {

	@Autowired
	private Environment environment;

	@Test
	void virtualThreadsShouldBeEnabled() {
		assertThat(environment.getProperty("spring.threads.virtual.enabled", Boolean.class)).isTrue();
	}
}

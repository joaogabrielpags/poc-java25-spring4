package com.pagbank.userregistration.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

/**
 * Confirma a configuração OTLP do starter (T4.1, D8/D22): contexto sobe sem
 * collector disponível (perfil {@code test} desabilita a exportação, ver
 * {@code application.yaml}), e as propriedades de sampling/tracing/metrics
 * resolvem com os prefixos corretos, confirmados empiricamente via bytecode de
 * {@code OpenTelemetryTracingProperties}/{@code OtlpMetricsProperties} (D22).
 */
@SpringBootTest
@ActiveProfiles("test")
class ObservabilityConfigurationTest {

	@Autowired
	private Environment environment;

	@Test
	void tracingSamplingProbabilityResolvesFromDefault() {
		assertThat(environment.getProperty("management.tracing.sampling.probability", Double.class))
				.isEqualTo(1.0);
	}

	@Test
	void otlpTracingEndpointResolvesFromDefault() {
		assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.endpoint"))
				.isEqualTo("http://localhost:4318/v1/traces");
	}

	@Test
	void otlpMetricsUrlResolvesFromDefault() {
		assertThat(environment.getProperty("management.otlp.metrics.export.url"))
				.isEqualTo("http://localhost:4318/v1/metrics");
	}

	@Test
	void otlpExportIsDisabledInTestProfile() {
		assertThat(environment.getProperty("management.opentelemetry.enabled", Boolean.class)).isFalse();
		assertThat(environment.getProperty("management.otlp.metrics.export.enabled", Boolean.class)).isFalse();
	}
}

package com.pagbank.userregistration.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.pagbank.userregistration.user.register.RegisterUserRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

/**
 * T4.4/D14: valida a correlação {@code trace_id}/{@code span_id} nos logs quando
 * {@code logging.structured.format.console=ecs} está ativo (perfil {@code local}).
 * Micrometer Tracing decora o MDC de todo log emitido dentro do escopo de um trace
 * ativo com as chaves {@code traceId}/{@code spanId} — o encoder ECS do Boot mapeia
 * essas chaves para os campos {@code trace.id}/{@code span.id} na saída JSON; aqui a
 * asserção é feita diretamente sobre o MDC do evento (independente do formato de
 * saída) para não acoplar o teste ao encoder.
 *
 * <p>{@code base-url} do ViaCEP é sobrescrito para um host que recusa conexão
 * imediatamente, mantendo o teste rápido e determinístico (mesmo padrão de
 * {@code RegisterAndFindUserIntegrationTest}, T2.7/T3.6).
 */
@SpringBootTest(
		webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.http.serviceclient.viacep.base-url=http://127.0.0.1:1")
@ActiveProfiles("local")
@AutoConfigureTestRestTemplate
class StructuredLoggingCorrelationTest {

	@Autowired
	private TestRestTemplate restTemplate;

	private Logger rootLogger;
	private ListAppender<ILoggingEvent> listAppender;

	@BeforeEach
	void attachAppender() {
		rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
		listAppender = new ListAppender<>();
		listAppender.start();
		rootLogger.addAppender(listAppender);
	}

	@AfterEach
	void detachAppender() {
		rootLogger.detachAppender(listAppender);
	}

	@Test
	void logEmittedDuringRequestShouldCarryTraceAndSpanIds() {
		RegisterUserRequest request = new RegisterUserRequest(
				"Usuário Teste", "logging." + UUID.randomUUID() + "@example.com", "70040-010");

		restTemplate.postForEntity("/api/v1/users", request, String.class);

		List<ILoggingEvent> events = listAppender.list;
		boolean anyEventCorrelated = events.stream().anyMatch(event -> {
			var mdc = event.getMDCPropertyMap();
			String traceId = mdc.get("traceId");
			String spanId = mdc.get("spanId");
			return traceId != null && !traceId.isBlank() && spanId != null && !spanId.isBlank();
		});

		assertThat(events).isNotEmpty();
		assertThat(anyEventCorrelated).isTrue();
	}
}

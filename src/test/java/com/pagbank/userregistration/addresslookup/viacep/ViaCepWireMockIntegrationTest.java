package com.pagbank.userregistration.addresslookup.viacep;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.pagbank.userregistration.user.register.RegisterUserRequest;
import com.pagbank.userregistration.user.register.RegisterUserResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Testes de integração com WireMock (T3.6) cobrindo os critérios 2 (enriquecimento),
 * 3 (fallback/circuit breaker) e 6 (CEP inexistente) do PRD, além da exposição dos
 * endpoints do actuator {@code circuitbreakers}/{@code circuitbreakerevents} (T3.7).
 *
 * <p>Perfil {@code test} habilita {@code app.http.allow-loopback} (ver ADR-006) para
 * que o cliente HTTP consiga chamar o WireMock em {@code localhost}. O
 * {@code base-url} do grupo {@code viacep} é sobrescrito para a porta dinâmica do
 * WireMock via {@link DynamicPropertySource}.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class ViaCepWireMockIntegrationTest {

	private static final WireMockServer WIRE_MOCK = new WireMockServer(WireMockConfiguration.options().dynamicPort());

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private CircuitBreakerRegistry circuitBreakerRegistry;

	@Autowired
	private MeterRegistry meterRegistry;

	@DynamicPropertySource
	static void viaCepBaseUrl(DynamicPropertyRegistry registry) {
		WIRE_MOCK.start();
		registry.add("spring.http.serviceclient.viacep.base-url", () -> "http://localhost:" + WIRE_MOCK.port());
	}

	@AfterAll
	static void stopWireMock() {
		WIRE_MOCK.stop();
	}

	@BeforeEach
	void resetState() {
		WIRE_MOCK.resetAll();
		circuitBreakerRegistry.circuitBreaker("viaCep").reset();
	}

	@Test
	void scenarioA_viaCepSuccessEnrichesAddress() {
		WIRE_MOCK.stubFor(get(urlMatching("/ws/.*/json/"))
				.willReturn(okJson(
						"""
						{"cep":"70040-010","logradouro":"Praça dos Três Poderes","bairro":"Zona Cívico-Administrativa","localidade":"Brasília","uf":"DF"}
						""")));
		double enrichedCountBefore = counterCount("user.registration.total", "enriched", "true");

		ResponseEntity<RegisterUserResponse> response = registerUser();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		RegisterUserResponse body = response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.addressEnriched()).isTrue();
		assertThat(body.address().street()).isEqualTo("Praça dos Três Poderes");
		assertThat(body.address().city()).isEqualTo("Brasília");
		// T4.3: user.registration.total{enriched=true} incrementado (D13).
		assertThat(counterCount("user.registration.total", "enriched", "true")).isEqualTo(enrichedCountBefore + 1);
	}

	@Test
	void scenarioB_tenFailuresOpenCircuitThenFallbackWithoutCallingWireMock() {
		WIRE_MOCK.stubFor(get(urlMatching("/ws/.*/json/")).willReturn(aResponse().withStatus(500)));
		double notEnrichedCountBefore = counterCount("user.registration.total", "enriched", "false");
		double fallbackCountBefore = meterRegistry.counter("user.registration.fallback").count();

		for (int i = 0; i < 10; i++) {
			ResponseEntity<RegisterUserResponse> response = registerUser();
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
			RegisterUserResponse body = response.getBody();
			assertThat(body).isNotNull();
			assertThat(body.addressEnriched()).isFalse();
		}

		assertThat(circuitBreakerRegistry.circuitBreaker("viaCep").getState()).isEqualTo(CircuitBreaker.State.OPEN);

		// T3.7: estado do circuito refletido no actuator (management.endpoints.web.exposure.include).
		ResponseEntity<String> actuatorResponse = restTemplate.getForEntity("/actuator/circuitbreakers", String.class);
		assertThat(actuatorResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(actuatorResponse.getBody()).contains("viaCep");
		ResponseEntity<String> actuatorEventsResponse =
				restTemplate.getForEntity("/actuator/circuitbreakerevents", String.class);
		assertThat(actuatorEventsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(actuatorEventsResponse.getBody()).contains("viaCep");

		ResponseEntity<RegisterUserResponse> nextAfterOpen = registerUser();
		assertThat(nextAfterOpen.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		RegisterUserResponse body = nextAfterOpen.getBody();
		assertThat(body).isNotNull();
		assertThat(body.addressEnriched()).isFalse();
		assertThat(body.address().cep()).isEqualTo("70040-010");
		assertThat(body.address().street()).isNull();

		WIRE_MOCK.verify(10, getRequestedFor(urlMatching("/ws/.*/json/")));
		// T4.3: 11 cadastros degradados (10 falhas técnicas + 1 circuito aberto) e o
		// mesmo número de incrementos em user.registration.fallback (T3.5).
		assertThat(counterCount("user.registration.total", "enriched", "false")).isEqualTo(notEnrichedCountBefore + 11);
		assertThat(meterRegistry.counter("user.registration.fallback").count()).isEqualTo(fallbackCountBefore + 11);
	}

	@Test
	void scenarioC_cepErroTrueReturns422AndKeepsCircuitClosed() {
		WIRE_MOCK.stubFor(get(urlMatching("/ws/.*/json/")).willReturn(okJson("{\"erro\": true}")));

		for (int i = 0; i < 10; i++) {
			ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
					"/api/v1/users",
					new RegisterUserRequest("Usuário Teste", uniqueEmail(), "70040-010"),
					ProblemDetail.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
		}

		assertThat(circuitBreakerRegistry.circuitBreaker("viaCep").getState()).isEqualTo(CircuitBreaker.State.CLOSED);
	}

	@Test
	void scenarioD_readTimeoutTriggersFallback() {
		WIRE_MOCK.stubFor(get(urlMatching("/ws/.*/json/"))
				.willReturn(okJson("{\"cep\":\"70040-010\"}").withFixedDelay(5000)));

		long start = System.currentTimeMillis();
		ResponseEntity<RegisterUserResponse> response = registerUser();
		long elapsedMillis = System.currentTimeMillis() - start;

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		RegisterUserResponse body = response.getBody();
		assertThat(body).isNotNull();
		assertThat(body.addressEnriched()).isFalse();
		// read-timeout configurado em 3s (D7); a resposta deve chegar bem antes do delay de 5s.
		assertThat(elapsedMillis).isLessThan(4500);
	}

	private ResponseEntity<RegisterUserResponse> registerUser() {
		RegisterUserRequest request = new RegisterUserRequest("Usuário Teste", uniqueEmail(), "70040-010");
		return restTemplate.postForEntity("/api/v1/users", request, RegisterUserResponse.class);
	}

	private double counterCount(String name, String tagKey, String tagValue) {
		var counter = meterRegistry.find(name).tag(tagKey, tagValue).counter();
		return counter == null ? 0.0 : counter.count();
	}

	private static String uniqueEmail() {
		return "wiremock." + UUID.randomUUID() + "@example.com";
	}
}

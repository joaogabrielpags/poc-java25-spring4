package com.pagbank.userregistration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.pagbank.userregistration.user.register.RegisterUserRequest;
import com.pagbank.userregistration.user.register.RegisterUserResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Teste de integração ponta a ponta do fluxo da F2 (T2.7): cadastro + consulta
 * (round-trip) com H2 real, e rejeição de e-mail duplicado.
 *
 * <p>Desde a F3 (T3.5), {@code AddressLookup} é implementado por
 * {@code ViaCepAddressLookup}, que faz uma chamada HTTP real. Para manter este teste
 * determinístico e sem dependência de rede (o cenário de enriquecimento real é coberto
 * por {@code ViaCepWireMockIntegrationTest}, T3.6), o {@code base-url} do grupo
 * {@code viacep} é sobrescrito para um host que recusa conexão imediatamente — a
 * primeira falha aciona o fallback do circuit breaker, resultando em
 * {@code addressEnriched=false}, como o teste já assumia.
 *
 * <p>Perfil {@code test} (T4.1) desabilita a exportação OTLP, evitando ruído de
 * conexão recusada no log já que não há collector disponível neste teste.
 */
@SpringBootTest(
		webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.http.serviceclient.viacep.base-url=http://127.0.0.1:1")
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class RegisterAndFindUserIntegrationTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void shouldRegisterAndFindUserRoundTrip() {
		RegisterUserRequest request = new RegisterUserRequest("Ana Silva", "ana.integracao@example.com", "70040-010");

		ResponseEntity<RegisterUserResponse> registerResponse =
				restTemplate.postForEntity("/api/v1/users", request, RegisterUserResponse.class);

		assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		RegisterUserResponse created = registerResponse.getBody();
		assertNotNull(created);
		UUID createdId = created.id();
		assertThat(registerResponse.getHeaders().getLocation())
				.hasPath("/api/v1/users/" + createdId);
		assertThat(created.addressEnriched()).isFalse();
		assertThat(created.address().cep()).isEqualTo("70040-010");
		assertThat(created.address().street()).isNull();

		ResponseEntity<String> findResponse =
				restTemplate.getForEntity("/api/v1/users/" + createdId, String.class);

		assertThat(findResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(findResponse.getBody()).contains("Ana Silva").contains("ana.integracao@example.com");
	}

	@Test
	void shouldRejectDuplicateEmailWith409() {
		RegisterUserRequest request =
				new RegisterUserRequest("Bruno Souza", "duplicado.integracao@example.com", "70040-010");

		ResponseEntity<RegisterUserResponse> first =
				restTemplate.postForEntity("/api/v1/users", request, RegisterUserResponse.class);
		assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		ResponseEntity<ProblemDetail> second =
				restTemplate.postForEntity("/api/v1/users", request, ProblemDetail.class);

		assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		ProblemDetail body = second.getBody();
		assertNotNull(body);
		assertThat(body.getType()).isNotNull();
		assertThat(body.getType().toString()).isEqualTo("urn:problem:user-already-exists");
	}
}

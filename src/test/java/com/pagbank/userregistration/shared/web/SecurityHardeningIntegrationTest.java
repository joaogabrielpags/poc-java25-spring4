package com.pagbank.userregistration.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Cobre T5.4 (RNF-07): hardening de segurança da API, verificado contra um servidor
 * embutido real (não {@code MockMvc}) para exercitar o comportamento do container:
 * resposta sem o header {@code Server} e rejeição de requisições com cabeçalhos HTTP
 * acima do limite configurado ({@code server.max-http-request-header-size}). O
 * cenário de erro 500 genérico sem vazamento de detalhes internos já é coberto por
 * {@link GlobalExceptionHandlerTest} via {@code @WebMvcTest}.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class SecurityHardeningIntegrationTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void responseShouldNotExposeServerHeader() {
		ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().get(HttpHeaders.SERVER)).isNull();
	}

	@Test
	void oversizedRequestHeaderShouldBeRejected() {
		HttpHeaders headers = new HttpHeaders();
		// server.max-http-request-header-size está fixado em 8KB (T5.4); um único
		// cabeçalho de ~16KB deve estourar o limite do container.
		headers.add("X-Huge-Header", "a".repeat(16_000));
		HttpEntity<Void> request = new HttpEntity<>(headers);

		ResponseEntity<String> response =
				restTemplate.exchange("/actuator/health", org.springframework.http.HttpMethod.GET, request, String.class);

		assertThat(response.getStatusCode().is4xxClientError()).isTrue();
	}
}

package com.pagbank.userregistration.shared.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pagbank.userregistration.addresslookup.Cep;
import com.pagbank.userregistration.addresslookup.CepNotFoundException;
import com.pagbank.userregistration.user.domain.Email;
import com.pagbank.userregistration.user.domain.UserAlreadyExistsException;
import com.pagbank.userregistration.user.domain.UserId;
import com.pagbank.userregistration.user.domain.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Cobre T2.4: mapeamento de exceções para Problem Details (RFC 9457). */
@WebMvcTest(controllers = ProbeController.class)
@ActiveProfiles("test-probe")
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProbeService probeService;

	@Test
	void userNotFoundShouldReturn404ProblemDetail() throws Exception {
		UserId id = UserId.newId();
		given(probeService.trigger(any())).willThrow(new UserNotFoundException(id));

		mockMvc.perform(get("/probe/user-not-found"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.type").value("urn:problem:user-not-found"))
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void userAlreadyExistsShouldReturn409ProblemDetail() throws Exception {
		given(probeService.trigger(any()))
				.willThrow(new UserAlreadyExistsException(new Email("ana@example.com")));

		mockMvc.perform(get("/probe/user-already-exists"))
				.andExpect(status().isConflict())
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.type").value("urn:problem:user-already-exists"))
				.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void cepNotFoundShouldReturn422ProblemDetail() throws Exception {
		given(probeService.trigger(any())).willThrow(new CepNotFoundException(new Cep("70040-010")));

		mockMvc.perform(get("/probe/cep-not-found"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.type").value("urn:problem:cep-not-found"))
				.andExpect(jsonPath("$.status").value(422));
	}

	@Test
	void unexpectedErrorShouldReturn500WithoutLeakingInternals() throws Exception {
		given(probeService.trigger(any())).willThrow(new IllegalStateException("segredo interno"));

		mockMvc.perform(get("/probe/unexpected"))
				.andExpect(status().isInternalServerError())
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.type").value("urn:problem:internal-error"))
				.andExpect(jsonPath("$.detail").value("Erro interno inesperado. Contate o suporte."));
	}
}

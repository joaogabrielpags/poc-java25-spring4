package com.pagbank.userregistration.user.register;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pagbank.userregistration.addresslookup.Cep;
import com.pagbank.userregistration.shared.web.GlobalExceptionHandler;
import com.pagbank.userregistration.user.domain.Address;
import com.pagbank.userregistration.user.domain.Email;
import com.pagbank.userregistration.user.domain.User;
import com.pagbank.userregistration.user.domain.UserAlreadyExistsException;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/** Cobre T2.5: validação (400), e-mail duplicado (409) e cadastro (201 + Location). */
@WebMvcTest(controllers = RegisterUserController.class)
@Import(GlobalExceptionHandler.class)
class RegisterUserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private RegisterUserUseCase useCase;

	private static final Clock CLOCK = Clock.system(ZoneOffset.UTC);

	@Test
	void shouldReturn201WithLocationOnSuccess() throws Exception {
		Address address = Address.cepOnly(new Cep("70040-010"));
		User user = User.register("Ana Silva", new Email("ana@example.com"), address, false, CLOCK);
		given(useCase.register(any())).willReturn(user);

		RegisterUserRequest request = new RegisterUserRequest("Ana Silva", "ana@example.com", "70040-010");

		mockMvc.perform(
						post("/api/v1/users")
								.contentType("application/json")
								.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/v1/users/" + user.id().value()))
				.andExpect(jsonPath("$.name").value("Ana Silva"))
				.andExpect(jsonPath("$.address.cep").value("70040-010"))
				.andExpect(jsonPath("$.address.street").doesNotExist());
	}

	@Test
	void shouldReturn400OnValidationFailure() throws Exception {
		RegisterUserRequest invalidRequest = new RegisterUserRequest("A", "not-an-email", "123");

		mockMvc.perform(
						post("/api/v1/users")
								.contentType("application/json")
								.content(objectMapper.writeValueAsString(invalidRequest)))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.errors").isArray());
	}

	@Test
	void shouldReturn409OnDuplicateEmail() throws Exception {
		given(useCase.register(any())).willThrow(new UserAlreadyExistsException(new Email("ana@example.com")));

		RegisterUserRequest request = new RegisterUserRequest("Ana Silva", "ana@example.com", "70040-010");

		mockMvc.perform(
						post("/api/v1/users")
								.contentType("application/json")
								.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isConflict())
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.type").value("urn:problem:user-already-exists"));
	}
}

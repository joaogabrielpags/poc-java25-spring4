package com.pagbank.userregistration.user.find;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pagbank.userregistration.addresslookup.Cep;
import com.pagbank.userregistration.shared.web.GlobalExceptionHandler;
import com.pagbank.userregistration.user.domain.Address;
import com.pagbank.userregistration.user.domain.Email;
import com.pagbank.userregistration.user.domain.User;
import com.pagbank.userregistration.user.domain.UserRepository;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Cobre T2.6: 200, 404 e UUID inválido → 400. */
@WebMvcTest(controllers = FindUserController.class)
@Import(GlobalExceptionHandler.class)
class FindUserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserRepository userRepository;

	private static final Clock CLOCK = Clock.system(ZoneOffset.UTC);

	@Test
	void shouldReturn200WhenUserExists() throws Exception {
		Address address = Address.cepOnly(new Cep("70040-010"));
		User user = User.register("Ana Silva", new Email("ana@example.com"), address, false, CLOCK);
		given(userRepository.findById(any())).willReturn(Optional.of(user));

		mockMvc.perform(get("/api/v1/users/{id}", user.id().value()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Ana Silva"))
				.andExpect(jsonPath("$.email").value("ana@example.com"));
	}

	@Test
	void shouldReturn404WhenUserNotFound() throws Exception {
		given(userRepository.findById(any())).willReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/users/{id}", UUID.randomUUID()))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType("application/problem+json"))
				.andExpect(jsonPath("$.type").value("urn:problem:user-not-found"));
	}

	@Test
	void shouldReturn400WhenIdIsNotAValidUuid() throws Exception {
		mockMvc.perform(get("/api/v1/users/{id}", "not-a-uuid")).andExpect(status().isBadRequest());
	}
}

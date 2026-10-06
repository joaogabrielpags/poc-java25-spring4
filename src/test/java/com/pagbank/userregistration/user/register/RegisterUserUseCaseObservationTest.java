package com.pagbank.userregistration.user.register;

import static io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.pagbank.userregistration.addresslookup.AddressLookup;
import com.pagbank.userregistration.addresslookup.AddressLookupResult;
import com.pagbank.userregistration.addresslookup.Cep;
import com.pagbank.userregistration.user.domain.Address;
import com.pagbank.userregistration.user.domain.UserAlreadyExistsException;
import com.pagbank.userregistration.user.domain.UserRepository;
import io.micrometer.observation.tck.TestObservationRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * T4.2: valida o span custom {@code user.registration} (nome contextual
 * {@code register-user}) e as tags de baixa cardinalidade {@code enriched}/
 * {@code outcome}, usando {@link TestObservationRegistry} (sem contexto Spring).
 */
class RegisterUserUseCaseObservationTest {

	private static final Clock FIXED_CLOCK =
			Clock.fixed(Instant.parse("2026-09-26T12:00:00Z"), ZoneOffset.UTC);

	private TestObservationRegistry observationRegistry;
	private UserRepository userRepository;
	private AddressLookup addressLookup;
	private RegisterUserUseCase useCase;

	@BeforeEach
	void setUp() {
		observationRegistry = TestObservationRegistry.create();
		userRepository = mock(UserRepository.class);
		addressLookup = mock(AddressLookup.class);
		ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
		useCase = new RegisterUserUseCase(
				userRepository, addressLookup, eventPublisher, FIXED_CLOCK, observationRegistry);
		given(userRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void shouldRecordObservationWithEnrichedTrueOnSuccess() {
		given(userRepository.existsByEmail(any())).willReturn(false);
		Address enrichedAddress = new Address(new Cep("70040-010"), "Rua Tal", "Bairro X", "Brasília", "DF");
		given(addressLookup.lookup(any())).willReturn(new AddressLookupResult.Found(enrichedAddress));

		useCase.register(new RegisterUserRequest("Ana Silva", "ana@example.com", "70040-010"));

		assertThat(observationRegistry)
				.hasObservationWithNameEqualTo("user.registration")
				.that()
				.hasContextualNameEqualTo("register-user")
				.hasLowCardinalityKeyValue("enriched", "true")
				.hasLowCardinalityKeyValue("outcome", "success")
				.hasBeenStopped();
	}

	@Test
	void shouldRecordObservationWithEnrichedFalseWhenDegraded() {
		given(userRepository.existsByEmail(any())).willReturn(false);
		given(addressLookup.lookup(any())).willReturn(new AddressLookupResult.Unavailable(new Cep("70040-010"), "Timeout"));

		useCase.register(new RegisterUserRequest("Ana Silva", "ana@example.com", "70040-010"));

		assertThat(observationRegistry)
				.hasObservationWithNameEqualTo("user.registration")
				.that()
				.hasLowCardinalityKeyValue("enriched", "false")
				.hasLowCardinalityKeyValue("outcome", "success")
				.hasBeenStopped();
	}

	@Test
	void shouldRecordErrorOutcomeWhenEmailAlreadyExists() {
		given(userRepository.existsByEmail(any())).willReturn(true);

		assertThatThrownBy(() -> useCase.register(
						new RegisterUserRequest("Ana Silva", "ana@example.com", "70040-010")))
				.isInstanceOf(UserAlreadyExistsException.class);

		assertThat(observationRegistry)
				.hasObservationWithNameEqualTo("user.registration")
				.that()
				.hasLowCardinalityKeyValue("outcome", "error")
				.hasError()
				.hasBeenStopped();
	}
}

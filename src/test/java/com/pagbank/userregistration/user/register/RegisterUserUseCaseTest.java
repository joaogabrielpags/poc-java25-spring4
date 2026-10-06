package com.pagbank.userregistration.user.register;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pagbank.userregistration.addresslookup.AddressLookup;
import com.pagbank.userregistration.addresslookup.AddressLookupResult;
import com.pagbank.userregistration.addresslookup.Cep;
import com.pagbank.userregistration.user.domain.Address;
import com.pagbank.userregistration.user.domain.Email;
import com.pagbank.userregistration.user.domain.User;
import com.pagbank.userregistration.user.domain.UserAlreadyExistsException;
import com.pagbank.userregistration.user.domain.UserRepository;
import io.micrometer.observation.ObservationRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class RegisterUserUseCaseTest {

	private static final Clock FIXED_CLOCK =
			Clock.fixed(Instant.parse("2026-09-26T12:00:00Z"), ZoneOffset.UTC);

	private UserRepository userRepository;
	private AddressLookup addressLookup;
	private ApplicationEventPublisher eventPublisher;
	private RegisterUserUseCase useCase;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		addressLookup = mock(AddressLookup.class);
		eventPublisher = mock(ApplicationEventPublisher.class);
		useCase = new RegisterUserUseCase(
				userRepository, addressLookup, eventPublisher, FIXED_CLOCK, ObservationRegistry.NOOP);
		given(userRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void shouldRejectWhenEmailAlreadyExists() {
		given(userRepository.existsByEmail(any())).willReturn(true);
		RegisterUserRequest request = new RegisterUserRequest("Ana Silva", "ana@example.com", "70040-010");

		assertThatThrownBy(() -> useCase.register(request)).isInstanceOf(UserAlreadyExistsException.class);
		verify(userRepository, never()).save(any());
	}

	@Test
	void shouldEnrichAddressWhenLookupSucceeds() {
		given(userRepository.existsByEmail(any())).willReturn(false);
		Address enrichedAddress =
				new Address(new Cep("70040-010"), "Rua Tal", "Bairro X", "Brasília", "DF");
		given(addressLookup.lookup(any())).willReturn(new AddressLookupResult.Found(enrichedAddress));
		RegisterUserRequest request = new RegisterUserRequest("Ana Silva", "ana@example.com", "70040-010");

		User user = useCase.register(request);

		assertThat(user.addressEnriched()).isTrue();
		assertThat(user.address()).isEqualTo(enrichedAddress);
		verify(eventPublisher).publishEvent(any(Object.class));
	}

	@Test
	void shouldDegradeToCepOnlyWhenLookupIsEmpty() {
		given(userRepository.existsByEmail(any())).willReturn(false);
		given(addressLookup.lookup(any())).willReturn(new AddressLookupResult.Unavailable(new Cep("70040-010"), "Timeout"));
		RegisterUserRequest request = new RegisterUserRequest("Ana Silva", "ana@example.com", "70040-010");

		User user = useCase.register(request);

		assertThat(user.addressEnriched()).isFalse();
		assertThat(user.address().street()).isNull();
		assertThat(user.address().cep().formatted()).isEqualTo("70040-010");
	}
}

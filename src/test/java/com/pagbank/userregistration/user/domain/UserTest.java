package com.pagbank.userregistration.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pagbank.userregistration.addresslookup.Cep;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class UserTest {

	private static final Clock FIXED_CLOCK =
			Clock.fixed(Instant.parse("2026-09-26T12:00:00Z"), ZoneOffset.UTC);

	@Test
	void registerShouldCreateUserWithGeneratedIdAndTimestamp() {
		Email email = new Email("ana@example.com");
		Address address = Address.cepOnly(new Cep("70040-010"));

		User user = User.register("Ana Silva", email, address, false, FIXED_CLOCK);

		assertThat(user.id()).isNotNull();
		assertThat(user.name()).isEqualTo("Ana Silva");
		assertThat(user.email()).isEqualTo(email);
		assertThat(user.address()).isEqualTo(address);
		assertThat(user.addressEnriched()).isFalse();
		assertThat(user.createdAt()).isEqualTo(Instant.parse("2026-09-26T12:00:00Z"));
	}

	@Test
	void shouldRejectNameShorterThanMinimum() {
		Email email = new Email("ana@example.com");
		Address address = Address.cepOnly(new Cep("70040-010"));

		assertThatThrownBy(() -> User.register("A", email, address, false, FIXED_CLOCK))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRejectNameLongerThanMaximum() {
		Email email = new Email("ana@example.com");
		Address address = Address.cepOnly(new Cep("70040-010"));
		String tooLong = "A".repeat(121);

		assertThatThrownBy(() -> User.register(tooLong, email, address, false, FIXED_CLOCK))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void reconstituteShouldPreserveGivenIdAndTimestamp() {
		UserId id = UserId.newId();
		Email email = new Email("ana@example.com");
		Address address = Address.cepOnly(new Cep("70040-010"));
		Instant createdAt = Instant.parse("2020-01-01T00:00:00Z");

		User user = User.reconstitute(id, "Ana Silva", email, address, true, createdAt);

		assertThat(user.id()).isEqualTo(id);
		assertThat(user.createdAt()).isEqualTo(createdAt);
		assertThat(user.addressEnriched()).isTrue();
	}
}

package com.pagbank.userregistration.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EmailTest {

	@Test
	void shouldNormalizeCaseAndWhitespace() {
		Email email = new Email("  Ana.Silva@Example.COM  ");
		assertThat(email.value()).isEqualTo("ana.silva@example.com");
	}

	@Test
	void shouldRejectInvalidFormat() {
		assertThatThrownBy(() -> new Email("not-an-email"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Email(""))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRejectNull() {
		assertThatThrownBy(() -> new Email(null))
				.isInstanceOf(NullPointerException.class);
	}
}

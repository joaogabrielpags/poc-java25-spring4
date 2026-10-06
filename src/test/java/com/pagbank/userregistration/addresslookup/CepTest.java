package com.pagbank.userregistration.addresslookup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CepTest {

	@Test
	void shouldAcceptDigitsOnly() {
		Cep cep = new Cep("70040010");
		assertThat(cep.digits()).isEqualTo("70040010");
		assertThat(cep.formatted()).isEqualTo("70040-010");
	}

	@Test
	void shouldAcceptHyphenatedFormat() {
		Cep cep = new Cep("70040-010");
		assertThat(cep.digits()).isEqualTo("70040010");
		assertThat(cep.formatted()).isEqualTo("70040-010");
	}

	@Test
	void shouldRejectInvalidCep() {
		assertThatThrownBy(() -> new Cep("123"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new Cep("abcdefgh"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRejectNull() {
		assertThatThrownBy(() -> new Cep(null))
				.isInstanceOf(NullPointerException.class);
	}
}

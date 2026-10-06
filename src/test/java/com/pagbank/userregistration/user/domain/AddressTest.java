package com.pagbank.userregistration.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pagbank.userregistration.addresslookup.Cep;
import org.junit.jupiter.api.Test;

class AddressTest {

	@Test
	void cepOnlyShouldLeaveOtherFieldsNull() {
		Cep cep = new Cep("70040-010");
		Address address = Address.cepOnly(cep);

		assertThat(address.cep()).isEqualTo(cep);
		assertThat(address.street()).isNull();
		assertThat(address.neighborhood()).isNull();
		assertThat(address.city()).isNull();
		assertThat(address.state()).isNull();
	}

	@Test
	void shouldRejectNullCep() {
		assertThatThrownBy(() -> new Address(null, null, null, null, null))
				.isInstanceOf(NullPointerException.class);
	}
}

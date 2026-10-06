package com.pagbank.userregistration.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserIdTest {

	@Test
	void newIdShouldGenerateDistinctValues() {
		assertThat(UserId.newId()).isNotEqualTo(UserId.newId());
	}

	@Test
	void shouldWrapUuid() {
		UUID uuid = UUID.randomUUID();
		assertThat(new UserId(uuid).value()).isEqualTo(uuid);
	}
}

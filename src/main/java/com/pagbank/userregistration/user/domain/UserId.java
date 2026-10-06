package com.pagbank.userregistration.user.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Identificador do aggregate {@link User}.
 *
 * @param value UUID subjacente.
 */
public record UserId(UUID value) {

	public UserId {
		Objects.requireNonNull(value, "value não pode ser nulo");
	}

	/** Gera um novo identificador aleatório. */
	public static UserId newId() {
		return new UserId(UUID.randomUUID());
	}
}

package com.pagbank.userregistration.user.domain;

import java.util.Objects;

/** Lançada quando um usuário não é encontrado por id (mapeada para 404). */
public final class UserNotFoundException extends RuntimeException {

	private final UserId userId;

	public UserNotFoundException(UserId id) {
		// Java 25 (feature): flexible constructor bodies (JEP 513) - código antes de super(...).
		Objects.requireNonNull(id, "id não pode ser nulo");
		this.userId = id;
		super("Usuário não encontrado: %s".formatted(id.value()));
	}

	public UserId userId() {
		return userId;
	}
}

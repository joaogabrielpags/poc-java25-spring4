package com.pagbank.userregistration.user.domain;

import java.util.Objects;

/** Lançada ao tentar cadastrar um usuário com e-mail já existente (mapeada para 409). */
public final class UserAlreadyExistsException extends RuntimeException {

	private final Email email;

	public UserAlreadyExistsException(Email email) {
		// Java 25 (feature): flexible constructor bodies (JEP 513) - código antes de super(...).
		Objects.requireNonNull(email, "email não pode ser nulo");
		this.email = email;
		super("Já existe um usuário cadastrado com o e-mail: %s".formatted(email.value()));
	}

	public Email email() {
		return email;
	}
}

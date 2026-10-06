package com.pagbank.userregistration.user.domain;

/** Lançada ao tentar cadastrar um usuário com e-mail já existente (mapeada para 409). */
public class UserAlreadyExistsException extends RuntimeException {

	public UserAlreadyExistsException(Email email) {
		super("Já existe um usuário cadastrado com o e-mail: %s".formatted(email.value()));
	}
}

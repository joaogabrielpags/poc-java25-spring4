package com.pagbank.userregistration.user.domain;

/** Lançada quando um usuário não é encontrado por id (mapeada para 404). */
public class UserNotFoundException extends RuntimeException {

	public UserNotFoundException(UserId id) {
		super("Usuário não encontrado: %s".formatted(id.value()));
	}
}

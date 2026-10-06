package com.pagbank.userregistration.user.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Aggregate raiz de usuário. Livre de anotações de framework (Spring/JPA) —
 * ver ADR-002. Instâncias são criadas via {@link #register} (novo cadastro)
 * ou {@link #reconstitute} (rehidratação a partir da persistência).
 *
 * @param id identificador do usuário.
 * @param name nome completo (2 a 120 caracteres).
 * @param email e-mail normalizado.
 * @param address endereço (completo ou apenas CEP, ver {@link Address}).
 * @param addressEnriched {@code true} quando o endereço foi enriquecido via ViaCEP.
 * @param createdAt instante de criação do registro.
 */
public record User(
		UserId id,
		String name,
		Email email,
		Address address,
		boolean addressEnriched,
		Instant createdAt) {

	private static final int NAME_MIN_LENGTH = 2;
	private static final int NAME_MAX_LENGTH = 120;

	public User {
		Objects.requireNonNull(id, "id não pode ser nulo");
		Objects.requireNonNull(name, "name não pode ser nulo");
		Objects.requireNonNull(email, "email não pode ser nulo");
		Objects.requireNonNull(address, "address não pode ser nulo");
		Objects.requireNonNull(createdAt, "createdAt não pode ser nulo");
		String trimmedName = name.strip();
		if (trimmedName.length() < NAME_MIN_LENGTH || trimmedName.length() > NAME_MAX_LENGTH) {
			throw new IllegalArgumentException(
					"name deve ter entre %d e %d caracteres".formatted(NAME_MIN_LENGTH, NAME_MAX_LENGTH));
		}
		name = trimmedName;
	}

	/** Cria um novo usuário no momento do cadastro. */
	public static User register(
			String name, Email email, Address address, boolean addressEnriched, Clock clock) {
		return new User(UserId.newId(), name, email, address, addressEnriched, Instant.now(clock));
	}

	/** Reconstrói um usuário a partir de dados já persistidos. */
	public static User reconstitute(
			UserId id,
			String name,
			Email email,
			Address address,
			boolean addressEnriched,
			Instant createdAt) {
		return new User(id, name, email, address, addressEnriched, createdAt);
	}
}

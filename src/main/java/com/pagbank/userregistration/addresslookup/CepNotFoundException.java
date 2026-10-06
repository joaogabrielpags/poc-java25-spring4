package com.pagbank.userregistration.addresslookup;

import java.util.Objects;

/**
 * Lançada pelo caso de uso quando o provedor de CEP confirma que o CEP não existe
 * ({@code AddressLookupResult.NotFound}). Erro de negócio, mapeado para 422.
 */
public final class CepNotFoundException extends RuntimeException {

	private final Cep cep;

	public CepNotFoundException(Cep cep) {
		// Java 25 (feature): flexible constructor bodies (JEP 513) - código antes de super(...).
		Objects.requireNonNull(cep, "cep não pode ser nulo");
		this.cep = cep;
		super("CEP não encontrado: %s".formatted(cep.formatted()));
	}

	public Cep cep() {
		return cep;
	}
}

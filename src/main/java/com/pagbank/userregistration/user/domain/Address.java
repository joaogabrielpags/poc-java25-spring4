package com.pagbank.userregistration.user.domain;

import com.pagbank.userregistration.addresslookup.Cep;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Endereço do usuário. Quando o enriquecimento via ViaCEP falha ou é
 * degradado (ver D10/D11 do backlog), apenas {@code cep} é preenchido e os
 * demais campos ficam {@code null} — use {@link #cepOnly(Cep)} nesse caso.
 *
 * @param cep CEP normalizado.
 * @param street logradouro (nulo quando não enriquecido).
 * @param neighborhood bairro (nulo quando não enriquecido).
 * @param city cidade (nulo quando não enriquecido).
 * @param state UF (nulo quando não enriquecido).
 */
public record Address(
		Cep cep,
		@Nullable String street,
		@Nullable String neighborhood,
		@Nullable String city,
		@Nullable String state) {

	public Address {
		Objects.requireNonNull(cep, "cep não pode ser nulo");
	}

	/** Cria um endereço contendo apenas o CEP (sem enriquecimento). */
	public static Address cepOnly(Cep cep) {
		return new Address(cep, null, null, null, null);
	}
}

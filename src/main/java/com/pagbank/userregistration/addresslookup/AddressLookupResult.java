package com.pagbank.userregistration.addresslookup;

import com.pagbank.userregistration.user.domain.Address;

/**
 * Resultado de {@link AddressLookup#lookup(Cep)}. Tipo selado: o {@code switch} do chamador
 * é verificado como exaustivo pelo compilador.
 */
// Java 25 (feature): sealed types (JEP 409) + records habilitam switch exaustivo com record patterns.
public sealed interface AddressLookupResult {

	/** Endereço encontrado e enriquecido pelo provedor. */
	record Found(Address address) implements AddressLookupResult {
	}

	/** O provedor confirmou que o CEP não existe (erro de negócio). */
	record NotFound(Cep cep) implements AddressLookupResult {
	}

	/** Falha técnica (timeout, 5xx, circuito aberto); o chamador deve degradar para CEP apenas. */
	record Unavailable(Cep cep, String reason) implements AddressLookupResult {
	}
}

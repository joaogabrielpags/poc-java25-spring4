package com.pagbank.userregistration.addresslookup;

/**
 * Port de enriquecimento de endereço a partir de um CEP. Implementada em
 * {@code addresslookup.viacep} (adapter ViaCEP com circuit breaker).
 *
 * <p>Os três desfechos possíveis são modelados por {@link AddressLookupResult}
 * ({@code Found}, {@code NotFound}, {@code Unavailable}); a port não lança exceções.
 */
public interface AddressLookup {

	AddressLookupResult lookup(Cep cep);
}

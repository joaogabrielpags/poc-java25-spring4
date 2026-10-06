package com.pagbank.userregistration.addresslookup;

import com.pagbank.userregistration.user.domain.Address;
import java.util.Optional;

/**
 * Port de enriquecimento de endereço a partir de um CEP. Implementada em
 * {@code addresslookup.viacep} (adapter ViaCEP com circuit breaker — ver F3).
 *
 * <p><b>Contrato de erros</b> (D10/D12):
 *
 * <ul>
 *   <li>CEP formalmente inexistente (provedor confirma {@code erro: true}) — lança
 *       {@link CepNotFoundException}. É um erro de negócio, não conta como falha do
 *       circuit breaker.
 *   <li>Falha técnica (timeout, 5xx, circuito aberto) — retorna {@link Optional#empty()};
 *       o chamador deve degradar para {@code Address.cepOnly(cep)} com
 *       {@code addressEnriched=false}.
 * </ul>
 */
public interface AddressLookup {

	Optional<Address> lookup(Cep cep);
}

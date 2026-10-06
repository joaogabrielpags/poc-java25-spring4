package com.pagbank.userregistration.addresslookup.viacep;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * HTTP Service Client declarativo (T3.3) do ViaCEP — interface gerenciada pela
 * infraestrutura de HTTP Service Clients do Spring (registrada em
 * {@link HttpClientsConfig} via {@code @ImportHttpServices}), sem implementação manual.
 */
@HttpExchange(url = "/ws", accept = MediaType.APPLICATION_JSON_VALUE)
public interface CepClient {

	@GetExchange("/{cep}/json/")
	ViaCepResponse findByCep(@PathVariable String cep);
}

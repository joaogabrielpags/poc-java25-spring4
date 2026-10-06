package com.pagbank.userregistration.addresslookup.viacep;

import com.pagbank.userregistration.addresslookup.AddressLookup;
import com.pagbank.userregistration.addresslookup.AddressLookupResult;
import com.pagbank.userregistration.addresslookup.AddressLookupResult.Found;
import com.pagbank.userregistration.addresslookup.AddressLookupResult.NotFound;
import com.pagbank.userregistration.addresslookup.AddressLookupResult.Unavailable;
import com.pagbank.userregistration.addresslookup.Cep;
import com.pagbank.userregistration.user.domain.Address;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Adapter de {@link AddressLookup} baseado no ViaCEP, protegido por circuit breaker
 * declarativo (ADR-005): {@code @CircuitBreaker(name = "viaCep")} com {@code fallbackMethod}.
 *
 * <p>{@code erro: true} do ViaCEP é retornado como {@link NotFound} (valor, não exceção), então
 * conta como sucesso para o circuito e dispensa {@code ignore-exceptions} e uma segunda
 * sobrecarga de fallback. Qualquer falha técnica (timeout, 5xx, host bloqueado por SSRF,
 * circuito aberto) aciona {@link #fallbackLookup(Cep, Throwable)}, que retorna
 * {@link Unavailable} e incrementa {@code user.registration.fallback} com a tag {@code reason}.
 */
@Component
public class ViaCepAddressLookup implements AddressLookup {

	private final CepClient cepClient;
	private final MeterRegistry meterRegistry;

	public ViaCepAddressLookup(CepClient cepClient, MeterRegistry meterRegistry) {
		this.cepClient = cepClient;
		this.meterRegistry = meterRegistry;
	}

	@Override
	@CircuitBreaker(name = "viaCep", fallbackMethod = "fallbackLookup")
	public AddressLookupResult lookup(Cep cep) {
		ViaCepResponse response = cepClient.findByCep(cep.digits());
		// Java 25 (feature): resultado como tipo selado (AddressLookupResult), sem Optional/exceção.
		return Boolean.TRUE.equals(response.erro()) ? new NotFound(cep) : new Found(toAddress(cep, response));
	}

	/** Invocado via reflection pelo Resilience4j quando {@link #lookup(Cep)} falha. */
	@SuppressWarnings("unused")
	private AddressLookupResult fallbackLookup(Cep cep, Throwable ex) {
		String reason = ex.getClass().getSimpleName();
		meterRegistry.counter("user.registration.fallback", "reason", reason).increment();
		return new Unavailable(cep, reason);
	}

	private static Address toAddress(Cep cep, ViaCepResponse response) {
		return new Address(cep, response.logradouro(), response.bairro(), response.localidade(), response.uf());
	}
}

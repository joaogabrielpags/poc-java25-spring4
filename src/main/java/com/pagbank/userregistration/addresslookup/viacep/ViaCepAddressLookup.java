package com.pagbank.userregistration.addresslookup.viacep;

import com.pagbank.userregistration.addresslookup.AddressLookup;
import com.pagbank.userregistration.addresslookup.Cep;
import com.pagbank.userregistration.addresslookup.CepNotFoundException;
import com.pagbank.userregistration.user.domain.Address;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapter de {@link AddressLookup} baseado no ViaCEP (T3.5), protegido por circuit
 * breaker declarativo (ver ADR-005, D18): {@code @CircuitBreaker(name = "viaCep")}
 * com {@code fallbackMethod}.
 *
 * <p>{@code erro: true} do ViaCEP é mapeado para {@link CepNotFoundException} (erro de
 * negócio); a instância {@code viaCep} está configurada com {@code ignore-exceptions:
 * [CepNotFoundException]} (D10), então essa exceção não conta como falha do circuito.
 * Isso por si só **não** impede o Resilience4j de rotear para
 * {@code fallbackMethod} (o {@code ignore-exceptions} só afeta a taxa de falhas do
 * circuito, não a seleção de fallback) — por isso existe uma sobrecarga de
 * {@link #fallbackLookup(Cep, CepNotFoundException)} mais específica que apenas
 * relança a exceção, deixando-a chegar ao {@code GlobalExceptionHandler} (422).
 *
 * <p>Qualquer outra falha (timeout, 5xx, host bloqueado por SSRF, circuito aberto)
 * aciona {@link #fallbackLookup(Cep, Throwable)}, que degrada para
 * {@link Optional#empty()} e incrementa {@code user.registration.fallback}.
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
	public Optional<Address> lookup(Cep cep) {
		ViaCepResponse response = cepClient.findByCep(cep.digits());
		if (Boolean.TRUE.equals(response.erro())) {
			throw new CepNotFoundException(cep);
		}
		return Optional.of(toAddress(cep, response));
	}

	/**
	 * Sobrecarga mais específica: {@link CepNotFoundException} é erro de negócio (D10),
	 * não uma falha técnica — deve se propagar para o {@code GlobalExceptionHandler}
	 * (422), não acionar o fallback genérico abaixo. O Resilience4j seleciona a
	 * sobrecarga de {@code fallbackMethod} cujo parâmetro {@link Throwable} é o mais
	 * específico compatível com a exceção lançada (por isso a ordem das duas sobrecargas
	 * aqui não importa, mas ambas precisam existir).
	 */
	@SuppressWarnings("unused")
	private Optional<Address> fallbackLookup(Cep cep, CepNotFoundException ex) {
		throw ex;
	}

	/** Invocado via reflection pelo Resilience4j quando {@link #lookup(Cep)} falha (D10). */
	@SuppressWarnings("unused")
	private Optional<Address> fallbackLookup(Cep cep, Throwable ex) {
		meterRegistry.counter("user.registration.fallback").increment();
		return Optional.empty();
	}

	private static Address toAddress(Cep cep, ViaCepResponse response) {
		return new Address(cep, response.logradouro(), response.bairro(), response.localidade(), response.uf());
	}
}

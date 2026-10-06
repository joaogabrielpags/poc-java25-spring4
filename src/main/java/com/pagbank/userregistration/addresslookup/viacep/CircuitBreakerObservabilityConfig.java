package com.pagbank.userregistration.addresslookup.viacep;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Configuration;

/**
 * T4.3: publica um {@link Observation} (evento pontual, sem span pai) toda vez que um
 * circuit breaker muda de estado (fechado → aberto → half-open), complementando as
 * métricas nativas do Resilience4j ({@code resilience4j.circuitbreaker.state}) já
 * expostas via {@code MeterRegistry}/actuator (T3.7).
 *
 * <p>Registrado para todo circuito já existente no registro e para qualquer um
 * adicionado depois, evitando acoplamento ao nome {@code viaCep}.
 */
@Configuration
public class CircuitBreakerObservabilityConfig {

	public CircuitBreakerObservabilityConfig(
			CircuitBreakerRegistry circuitBreakerRegistry, ObservationRegistry observationRegistry) {
		circuitBreakerRegistry.getAllCircuitBreakers()
				.forEach(circuitBreaker -> registerStateTransitionObservation(circuitBreaker, observationRegistry));
		circuitBreakerRegistry.getEventPublisher()
				.onEntryAdded(event -> registerStateTransitionObservation(event.getAddedEntry(), observationRegistry));
	}

	private static void registerStateTransitionObservation(
			CircuitBreaker circuitBreaker, ObservationRegistry observationRegistry) {
		circuitBreaker.getEventPublisher().onStateTransition(event -> Observation
				.createNotStarted("circuit.state.changed", observationRegistry)
				.lowCardinalityKeyValue("circuitbreaker.name", event.getCircuitBreakerName())
				.lowCardinalityKeyValue("from.state", event.getStateTransition().getFromState().name())
				.lowCardinalityKeyValue("to.state", event.getStateTransition().getToState().name())
				.start()
				.stop());
	}
}

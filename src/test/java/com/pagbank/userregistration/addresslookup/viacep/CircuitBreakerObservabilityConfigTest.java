package com.pagbank.userregistration.addresslookup.viacep;

import static io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;

/**
 * T4.3: valida que uma transição de estado do circuit breaker produz um
 * {@link io.micrometer.observation.Observation} {@code circuit.state.changed} com as
 * tags de nome/estados.
 */
class CircuitBreakerObservabilityConfigTest {

	@Test
	void shouldRecordObservationOnStateTransition() {
		CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
		registry.circuitBreaker("viaCep");
		TestObservationRegistry observationRegistry = TestObservationRegistry.create();
		new CircuitBreakerObservabilityConfig(registry, observationRegistry);

		registry.circuitBreaker("viaCep").transitionToOpenState();

		assertThat(observationRegistry)
				.hasObservationWithNameEqualTo("circuit.state.changed")
				.that()
				.hasLowCardinalityKeyValue("circuitbreaker.name", "viaCep")
				.hasLowCardinalityKeyValue("to.state", CircuitBreaker.State.OPEN.name())
				.hasBeenStopped();
	}

	@Test
	void shouldRegisterListenerOnCircuitBreakersAddedAfterConstruction() {
		CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
		TestObservationRegistry observationRegistry = TestObservationRegistry.create();
		new CircuitBreakerObservabilityConfig(registry, observationRegistry);

		registry.circuitBreaker("createdLater").transitionToOpenState();

		assertThat(observationRegistry)
				.hasObservationWithNameEqualTo("circuit.state.changed")
				.that()
				.hasLowCardinalityKeyValue("circuitbreaker.name", "createdLater");
	}
}

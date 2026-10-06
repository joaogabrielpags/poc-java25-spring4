package com.pagbank.userregistration.user.register;

import com.pagbank.userregistration.user.domain.UserRegistered;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * T4.3/D13: incrementa {@code user.registration.total{enriched=true|false}} sempre
 * que um {@link UserRegistered} é publicado pelo {@link RegisterUserUseCase}.
 */
@Component
public class UserRegistrationMetricsListener {

	private final MeterRegistry meterRegistry;

	public UserRegistrationMetricsListener(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	@EventListener
	public void onUserRegistered(UserRegistered event) {
		meterRegistry.counter("user.registration.total", "enriched", String.valueOf(event.enriched()))
				.increment();
	}
}

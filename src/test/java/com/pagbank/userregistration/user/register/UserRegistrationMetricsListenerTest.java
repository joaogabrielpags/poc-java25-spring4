package com.pagbank.userregistration.user.register;

import static org.assertj.core.api.Assertions.assertThat;

import com.pagbank.userregistration.user.domain.UserId;
import com.pagbank.userregistration.user.domain.UserRegistered;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * T4.3/D13: {@code user.registration.total} incrementado por {@code enriched}.
 */
class UserRegistrationMetricsListenerTest {

	@Test
	void shouldIncrementCounterTaggedByEnrichedFlag() {
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
		UserRegistrationMetricsListener listener = new UserRegistrationMetricsListener(meterRegistry);

		listener.onUserRegistered(new UserRegistered(new UserId(UUID.randomUUID()), true, Instant.now()));
		listener.onUserRegistered(new UserRegistered(new UserId(UUID.randomUUID()), false, Instant.now()));
		listener.onUserRegistered(new UserRegistered(new UserId(UUID.randomUUID()), false, Instant.now()));

		assertThat(meterRegistry.counter("user.registration.total", "enriched", "true").count()).isEqualTo(1.0);
		assertThat(meterRegistry.counter("user.registration.total", "enriched", "false").count()).isEqualTo(2.0);
	}
}

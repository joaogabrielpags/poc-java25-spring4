package com.pagbank.userregistration.shared.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Beans de infraestrutura transversal (ver T2.5: {@link Clock} para {@code User.register}). */
@Configuration
public class ApplicationConfig {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}

package com.pagbank.userregistration.user.register;

import com.pagbank.userregistration.addresslookup.AddressLookup;
import com.pagbank.userregistration.addresslookup.Cep;
import com.pagbank.userregistration.user.domain.Address;
import com.pagbank.userregistration.user.domain.Email;
import com.pagbank.userregistration.user.domain.User;
import com.pagbank.userregistration.user.domain.UserAlreadyExistsException;
import com.pagbank.userregistration.user.domain.UserRegistered;
import com.pagbank.userregistration.user.domain.UserRepository;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de cadastro de usuário (T2.5). Enriquece o endereço via
 * {@link AddressLookup}; quando indisponível, degrada para
 * {@code Address.cepOnly(cep)} com {@code addressEnriched=false} (D10/D11).
 * Publica {@link UserRegistered} após persistir com sucesso (D13).
 *
 * <p>T4.2: todo o fluxo é envolvido por um {@link Observation} custom
 * {@code user.registration} (span + timer), com tags de baixa cardinalidade
 * {@code enriched} e {@code outcome}. Instrumentação manual via
 * {@link ObservationRegistry} (em vez de {@code @Observed}) porque as tags
 * dependem do resultado do cadastro, não são estáticas.
 */
@Service
public class RegisterUserUseCase {

	private static final Logger log = LoggerFactory.getLogger(RegisterUserUseCase.class);

	private final UserRepository userRepository;
	private final AddressLookup addressLookup;
	private final ApplicationEventPublisher eventPublisher;
	private final Clock clock;
	private final ObservationRegistry observationRegistry;

	public RegisterUserUseCase(
			UserRepository userRepository,
			AddressLookup addressLookup,
			ApplicationEventPublisher eventPublisher,
			Clock clock,
			ObservationRegistry observationRegistry) {
		this.userRepository = userRepository;
		this.addressLookup = addressLookup;
		this.eventPublisher = eventPublisher;
		this.clock = clock;
		this.observationRegistry = observationRegistry;
	}

	@Transactional
	public User register(RegisterUserRequest request) {
		Observation observation = Observation.createNotStarted("user.registration", observationRegistry)
				.contextualName("register-user")
				.start();
		try (Observation.Scope scope = observation.openScope()) {
			User user = doRegister(request);
			observation.lowCardinalityKeyValue("enriched", String.valueOf(user.addressEnriched()));
			observation.lowCardinalityKeyValue("outcome", "success");
			// T4.4: log estruturado (ECS em local/prod) correlacionado ao trace/span ativo.
			log.info("Cadastro de usuário concluído (userId={}, enriched={})", user.id(), user.addressEnriched());
			return user;
		}
		catch (RuntimeException ex) {
			observation.lowCardinalityKeyValue("outcome", "error");
			observation.error(ex);
			throw ex;
		}
		finally {
			observation.stop();
		}
	}

	private User doRegister(RegisterUserRequest request) {
		Email email = new Email(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new UserAlreadyExistsException(email);
		}

		Cep cep = new Cep(request.cep());
		boolean enriched = false;
		Address address = Address.cepOnly(cep);
		var lookupResult = addressLookup.lookup(cep);
		if (lookupResult.isPresent()) {
			address = lookupResult.orElseThrow();
			enriched = true;
		}

		User user = User.register(request.name(), email, address, enriched, clock);
		User saved = userRepository.save(user);
		eventPublisher.publishEvent(new UserRegistered(saved.id(), saved.addressEnriched(), saved.createdAt()));
		return saved;
	}
}


package com.pagbank.userregistration.shared.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pagbank.userregistration.addresslookup.Cep;
import com.pagbank.userregistration.user.domain.Address;
import com.pagbank.userregistration.user.domain.Email;
import com.pagbank.userregistration.user.domain.User;
import com.pagbank.userregistration.user.domain.UserId;
import com.pagbank.userregistration.user.domain.UserRepository;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/** Cobre T2.3: save/find/existsByEmail e violação de unicidade de e-mail. */
@DataJpaTest
@Import({UserJpaEntityMapper.class, JpaUserRepository.class})
class JpaUserRepositoryTest {

	@Autowired
	private UserRepository userRepository;

	private final Clock clock = Clock.system(ZoneOffset.UTC);

	@Test
	void shouldSaveAndFindById() {
		User user = registerSampleUser("ana@example.com");

		User saved = userRepository.save(user);
		Optional<User> found = userRepository.findById(saved.id());

		assertThat(found).isPresent();
		assertThat(found.orElseThrow().email()).isEqualTo(user.email());
		assertThat(found.orElseThrow().name()).isEqualTo(user.name());
	}

	@Test
	void findByIdShouldReturnEmptyWhenNotFound() {
		assertThat(userRepository.findById(UserId.newId())).isEmpty();
	}

	@Test
	void existsByEmailShouldReflectPersistedUsers() {
		User user = registerSampleUser("existente@example.com");
		userRepository.save(user);

		assertThat(userRepository.existsByEmail(new Email("existente@example.com"))).isTrue();
		assertThat(userRepository.existsByEmail(new Email("outro@example.com"))).isFalse();
	}

	@Test
	void shouldRejectDuplicateEmail() {
		userRepository.save(registerSampleUser("duplicado@example.com"));

		assertThatThrownBy(() -> userRepository.save(registerSampleUser("duplicado@example.com")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private User registerSampleUser(String email) {
		Address address = Address.cepOnly(new Cep("70040-010"));
		return User.register("Ana Silva", new Email(email), address, false, clock);
	}
}

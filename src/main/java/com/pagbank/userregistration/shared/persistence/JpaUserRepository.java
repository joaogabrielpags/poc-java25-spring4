package com.pagbank.userregistration.shared.persistence;

import com.pagbank.userregistration.user.domain.Email;
import com.pagbank.userregistration.user.domain.User;
import com.pagbank.userregistration.user.domain.UserId;
import com.pagbank.userregistration.user.domain.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapter que implementa a port {@link UserRepository} do domínio usando
 * {@link SpringDataUserJpaRepository} — única dependência permitida de
 * {@code shared} sobre {@code user.domain} (ver ADR-002).
 */
@Component
public class JpaUserRepository implements UserRepository {

	private final SpringDataUserJpaRepository springDataRepository;
	private final UserJpaEntityMapper mapper;

	public JpaUserRepository(SpringDataUserJpaRepository springDataRepository, UserJpaEntityMapper mapper) {
		this.springDataRepository = springDataRepository;
		this.mapper = mapper;
	}

	@Override
	public User save(User user) {
		// saveAndFlush força a checagem da constraint de unicidade de e-mail
		// imediatamente, garantindo que violações sejam traduzidas para
		// DataIntegrityViolationException dentro do próprio use case (409).
		UserJpaEntity saved = springDataRepository.saveAndFlush(mapper.toEntity(user));
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<User> findById(UserId id) {
		return springDataRepository.findById(id.value()).map(mapper::toDomain);
	}

	@Override
	public boolean existsByEmail(Email email) {
		return springDataRepository.existsByEmail(email.value());
	}
}

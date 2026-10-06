package com.pagbank.userregistration.user.domain;

import java.util.Optional;

/**
 * Port de persistência do aggregate {@link User}. Implementada em
 * {@code shared.persistence} (ver ADR-002).
 */
public interface UserRepository {

	User save(User user);

	Optional<User> findById(UserId id);

	boolean existsByEmail(Email email);
}

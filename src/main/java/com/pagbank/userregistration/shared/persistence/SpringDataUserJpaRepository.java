package com.pagbank.userregistration.shared.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data JPA para {@link UserJpaEntity} (infraestrutura). */
public interface SpringDataUserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

	Optional<UserJpaEntity> findByEmail(String email);

	boolean existsByEmail(String email);
}

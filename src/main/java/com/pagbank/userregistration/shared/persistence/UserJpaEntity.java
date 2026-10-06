package com.pagbank.userregistration.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Modelo de tabela JPA para {@code users}. Mapeado a partir/para o aggregate
 * {@code User} (livre de framework) via {@link UserJpaEntityMapper} — ver ADR-002.
 */
@Entity
@Table(name = "users")
@SuppressWarnings("NullAway.Init") // campos preenchidos via construtor de negócio ou reflection do Hibernate
public class UserJpaEntity {

	@Id
	private UUID id;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false, length = 8)
	private String cep;

	@Nullable
	private String street;

	@Nullable
	private String neighborhood;

	@Nullable
	private String city;

	@Nullable
	private String state;

	@Column(nullable = false)
	private boolean addressEnriched;

	@Column(nullable = false)
	private Instant createdAt;

	/** Construtor exigido pelo Hibernate. */
	protected UserJpaEntity() {
	}

	public UserJpaEntity(
			UUID id,
			String name,
			String email,
			String cep,
			@Nullable String street,
			@Nullable String neighborhood,
			@Nullable String city,
			@Nullable String state,
			boolean addressEnriched,
			Instant createdAt) {
		this.id = id;
		this.name = name;
		this.email = email;
		this.cep = cep;
		this.street = street;
		this.neighborhood = neighborhood;
		this.city = city;
		this.state = state;
		this.addressEnriched = addressEnriched;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getCep() {
		return cep;
	}

	@Nullable
	public String getStreet() {
		return street;
	}

	@Nullable
	public String getNeighborhood() {
		return neighborhood;
	}

	@Nullable
	public String getCity() {
		return city;
	}

	@Nullable
	public String getState() {
		return state;
	}

	public boolean isAddressEnriched() {
		return addressEnriched;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}

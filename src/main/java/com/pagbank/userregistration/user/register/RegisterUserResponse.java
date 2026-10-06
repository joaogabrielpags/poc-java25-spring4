package com.pagbank.userregistration.user.register;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pagbank.userregistration.user.domain.User;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Corpo da resposta de {@code POST /api/v1/users} (201 Created).
 *
 * <p>Quando {@code addressEnriched=false}, {@code address} contém apenas {@code cep}
 * normalizado; os demais campos são omitidos do JSON (D11).
 */
public record RegisterUserResponse(
		UUID id, String name, String email, AddressView address, boolean addressEnriched, Instant createdAt) {

	public static RegisterUserResponse from(User user) {
		var address = user.address();
		AddressView addressView =
				new AddressView(
						address.cep().formatted(),
						address.street(),
						address.neighborhood(),
						address.city(),
						address.state());
		return new RegisterUserResponse(
				user.id().value(),
				user.name(),
				user.email().value(),
				addressView,
				user.addressEnriched(),
				user.createdAt());
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record AddressView(
			String cep,
			@Nullable String street,
			@Nullable String neighborhood,
			@Nullable String city,
			@Nullable String state) {
	}
}

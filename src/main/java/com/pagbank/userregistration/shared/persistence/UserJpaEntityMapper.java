package com.pagbank.userregistration.shared.persistence;

import com.pagbank.userregistration.addresslookup.Cep;
import com.pagbank.userregistration.user.domain.Address;
import com.pagbank.userregistration.user.domain.Email;
import com.pagbank.userregistration.user.domain.User;
import com.pagbank.userregistration.user.domain.UserId;
import org.springframework.stereotype.Component;

/** Converte entre o aggregate {@code User} (livre de framework) e {@link UserJpaEntity}. */
@Component
public class UserJpaEntityMapper {

	public UserJpaEntity toEntity(User user) {
		Address address = user.address();
		return new UserJpaEntity(
				user.id().value(),
				user.name(),
				user.email().value(),
				address.cep().digits(),
				address.street(),
				address.neighborhood(),
				address.city(),
				address.state(),
				user.addressEnriched(),
				user.createdAt());
	}

	public User toDomain(UserJpaEntity entity) {
		Address address =
				new Address(
						new Cep(entity.getCep()),
						entity.getStreet(),
						entity.getNeighborhood(),
						entity.getCity(),
						entity.getState());
		return User.reconstitute(
				new UserId(entity.getId()),
				entity.getName(),
				new Email(entity.getEmail()),
				address,
				entity.isAddressEnriched(),
				entity.getCreatedAt());
	}
}

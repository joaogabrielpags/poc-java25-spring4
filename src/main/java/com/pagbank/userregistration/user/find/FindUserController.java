package com.pagbank.userregistration.user.find;

import com.pagbank.userregistration.user.domain.User;
import com.pagbank.userregistration.user.domain.UserId;
import com.pagbank.userregistration.user.domain.UserNotFoundException;
import com.pagbank.userregistration.user.domain.UserRepository;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/v1/users/{id}} — consulta de usuário por id (T2.6). */
@RestController
@RequestMapping("/api/v1/users")
public class FindUserController {

	private final UserRepository userRepository;

	public FindUserController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@GetMapping("/{id}")
	public FindUserResponse find(@PathVariable UUID id) {
		UserId userId = new UserId(id);
		User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
		return FindUserResponse.from(user);
	}
}

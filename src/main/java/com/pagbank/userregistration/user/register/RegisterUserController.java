package com.pagbank.userregistration.user.register;

import com.pagbank.userregistration.user.domain.User;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code POST /api/v1/users} — cadastro de usuário (T2.5). */
@RestController
@RequestMapping("/api/v1/users")
public class RegisterUserController {

	private final RegisterUserUseCase useCase;

	public RegisterUserController(RegisterUserUseCase useCase) {
		this.useCase = useCase;
	}

	@PostMapping
	public ResponseEntity<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
		User user = useCase.register(request);
		RegisterUserResponse response = RegisterUserResponse.from(user);
		URI location = URI.create("/api/v1/users/" + user.id().value());
		return ResponseEntity.created(location).body(response);
	}
}

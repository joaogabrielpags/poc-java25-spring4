package com.pagbank.userregistration.user.register;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Corpo da requisição de {@code POST /api/v1/users}.
 *
 * @param name nome completo (2 a 120 caracteres).
 * @param email e-mail do usuário.
 * @param cep CEP com ou sem hífen ({@code "70040010"} ou {@code "70040-010"}).
 */
public record RegisterUserRequest(
		@NotBlank @Size(min = 2, max = 120) String name,
		@NotBlank @Email String email,
		@NotBlank @Pattern(regexp = "\\d{8}|\\d{5}-\\d{3}") String cep) {
}

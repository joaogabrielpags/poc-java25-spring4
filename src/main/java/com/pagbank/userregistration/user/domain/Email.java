package com.pagbank.userregistration.user.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object de e-mail. Normaliza (trim + lowercase) e valida o formato no
 * construtor compacto.
 *
 * @param value e-mail normalizado.
 */
public record Email(String value) {

	// RFC 5322 simplificada — suficiente para validação de entrada de usuário.
	private static final Pattern EMAIL_PATTERN =
			Pattern.compile("^[\\w+.-]+@[\\w-]+(\\.[\\w-]+)+$");

	public Email {
		Objects.requireNonNull(value, "value não pode ser nulo");
		value = value.strip().toLowerCase(Locale.ROOT);
		if (value.isEmpty() || !EMAIL_PATTERN.matcher(value).matches()) {
			throw new IllegalArgumentException("E-mail inválido: '%s'".formatted(value));
		}
	}
}

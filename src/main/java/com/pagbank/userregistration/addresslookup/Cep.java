package com.pagbank.userregistration.addresslookup;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object de CEP (Código de Endereçamento Postal).
 *
 * <p>Aceita entrada com ou sem hífen ({@code "70040010"} ou {@code "70040-010"}) e
 * normaliza internamente para os 8 dígitos. Expõe {@link #digits()} (sem máscara)
 * e {@link #formatted()} ({@code "70040-010"}).
 *
 * @param digits CEP normalizado com exatamente 8 dígitos numéricos.
 */
public record Cep(String digits) {

	private static final Pattern DIGITS_ONLY = Pattern.compile("\\d{8}");
	private static final Pattern WITH_HYPHEN = Pattern.compile("(\\d{5})-(\\d{3})");

	public Cep {
		Objects.requireNonNull(digits, "digits não pode ser nulo");
		String normalized = normalize(digits);
		if (!DIGITS_ONLY.matcher(normalized).matches()) {
			throw new IllegalArgumentException("CEP inválido: '%s'".formatted(digits));
		}
		digits = normalized;
	}

	private static String normalize(String raw) {
		String trimmed = raw.strip();
		var hyphenMatcher = WITH_HYPHEN.matcher(trimmed);
		if (hyphenMatcher.matches()) {
			return hyphenMatcher.group(1) + hyphenMatcher.group(2);
		}
		return trimmed;
	}

	/** Retorna o CEP formatado como {@code "70040-010"}. */
	public String formatted() {
		return digits.substring(0, 5) + "-" + digits.substring(5);
	}
}

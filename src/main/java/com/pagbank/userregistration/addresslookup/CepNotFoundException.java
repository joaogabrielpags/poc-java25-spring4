package com.pagbank.userregistration.addresslookup;

/**
 * Lançada quando o provedor de CEP (ex.: ViaCEP) confirma que o CEP não existe
 * (retorno {@code erro: true}). É um erro de negócio — mapeado para 422 — e,
 * por decisão D10, não conta como falha técnica para o circuit breaker
 * (ver {@code ignore-exceptions} em T3.5).
 */
public class CepNotFoundException extends RuntimeException {

	public CepNotFoundException(Cep cep) {
		super("CEP não encontrado: %s".formatted(cep.formatted()));
	}
}

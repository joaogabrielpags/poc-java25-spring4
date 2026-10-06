package com.pagbank.userregistration.addresslookup.viacep;

import org.jspecify.annotations.Nullable;

/**
 * Resposta do endpoint {@code https://viacep.com.br/ws/{cep}/json/} (T3.3).
 *
 * <p>Quando o CEP não existe, o ViaCEP retorna {@code {"erro": true}} e omite os
 * demais campos — por isso todos, exceto {@code erro}, são {@code @Nullable}.
 *
 * @param cep CEP formatado como retornado pelo provedor (ex.: {@code "70040-010"}).
 * @param logradouro nome da rua/avenida.
 * @param bairro bairro.
 * @param localidade cidade.
 * @param uf sigla do estado.
 * @param erro {@code true} quando o CEP é formalmente inexistente; {@code null}/{@code false} caso contrário.
 */
public record ViaCepResponse(
		@Nullable String cep,
		@Nullable String logradouro,
		@Nullable String bairro,
		@Nullable String localidade,
		@Nullable String uf,
		@Nullable Boolean erro) {}

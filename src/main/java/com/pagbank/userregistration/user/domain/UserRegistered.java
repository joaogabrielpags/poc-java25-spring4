package com.pagbank.userregistration.user.domain;

import java.time.Instant;

/**
 * Evento de domínio publicado após o cadastro bem-sucedido de um usuário
 * (ver D13). Consumido por um listener em {@code user.register} para
 * incrementar {@code user.registration.total{enriched}}.
 *
 * @param userId identificador do usuário cadastrado.
 * @param enriched se o endereço foi enriquecido via ViaCEP.
 * @param occurredAt instante do cadastro.
 */
public record UserRegistered(UserId userId, boolean enriched, Instant occurredAt) {
}

package com.pagbank.userregistration.shared.web;

/**
 * Serviço injetado em {@link ProbeController}, mockado nos testes de
 * {@code GlobalExceptionHandler}.
 */
public interface ProbeService {

	String trigger(String scenario);
}

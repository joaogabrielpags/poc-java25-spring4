package com.pagbank.userregistration.shared.web;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller mínimo usado apenas por {@code GlobalExceptionHandlerTest} para
 * exercitar o {@code GlobalExceptionHandler} via {@code MockMvc}. Fica restrito
 * ao perfil {@code test-probe} (ativado somente nesse teste de slice via
 * {@code @ActiveProfiles}) para não ser registrado quando o contexto completo
 * da aplicação é carregado (ex.: {@code @SpringBootTest}), já que não existe um
 * bean real de {@link ProbeService} fora dos testes de slice.
 */
@RestController
@Profile("test-probe")
public class ProbeController {

	private final ProbeService probeService;

	public ProbeController(ProbeService probeService) {
		this.probeService = probeService;
	}

	@GetMapping("/probe/{scenario}")
	public String probe(@PathVariable String scenario) {
		return probeService.trigger(scenario);
	}
}

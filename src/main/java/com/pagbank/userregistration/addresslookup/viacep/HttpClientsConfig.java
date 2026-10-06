package com.pagbank.userregistration.addresslookup.viacep;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.InetAddressFilter;
import org.springframework.boot.http.client.autoconfigure.HttpClientSettingsPropertyMapper;
import org.springframework.boot.http.client.autoconfigure.service.HttpServiceClientProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * Registro do HTTP Service Client do ViaCEP (T3.3) e hardening contra SSRF (T3.4,
 * RNF-07, ver ADR-006).
 *
 * <p>Fica em {@code addresslookup.viacep} (e não em {@code shared.config}, como o
 * PRD original sugeria) para não violar a regra "{@code shared} nunca importa
 * pacotes de feature" — ver decisão D19 em {@code docs/TASKS.md}.
 */
@Configuration
@ImportHttpServices(group = "viacep", types = CepClient.class)
public class HttpClientsConfig {

	private static final String VIA_CEP_GROUP = "viacep";

	/**
	 * Substitui o {@link org.springframework.http.client.ClientHttpRequestFactory} do
	 * grupo {@code viacep} por um configurado com {@link InetAddressFilter}, mantendo os
	 * demais ajustes (base-url, timeouts, SSL) já lidos de
	 * {@code spring.http.serviceclient.viacep.*} — não há propriedade YAML para o
	 * filtro de endereço, então a combinação é feita programaticamente via
	 * {@link HttpClientSettingsPropertyMapper} (mesma classe usada internamente pela
	 * auto-configuração do Boot).
	 *
	 * <p>{@code app.http.allow-loopback=true} (habilitado apenas no perfil {@code test})
	 * desativa o bloqueio para permitir WireMock em {@code localhost} (T3.6).
	 */
	@Bean
	RestClientHttpServiceGroupConfigurer viaCepSsrfHardeningConfigurer(
			HttpServiceClientProperties httpServiceClientProperties,
			SslBundles sslBundles,
			@Value("${app.http.allow-loopback:false}") boolean allowLoopback) {
		InetAddressFilter filter =
				allowLoopback ? InetAddressFilter.all() : InetAddressFilter.externalAddresses();
		return groups -> groups.filterByName(VIA_CEP_GROUP)
				.forEachClient((group, builder) -> {
					HttpClientSettings base = HttpClientSettings.defaults().withInetAddressFilter(filter);
					HttpClientSettings settings = new HttpClientSettingsPropertyMapper(sslBundles, base)
							.map(httpServiceClientProperties.get(group.name()));
					builder.requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings));
				});
	}
}

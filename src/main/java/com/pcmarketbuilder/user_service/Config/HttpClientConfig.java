package com.pcmarketbuilder.user_service.Config;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Cliente HTTP saliente para llamadas a otros microservicios (por ahora,
 * solo publication-service, ver Client/PublicationClient). Timeouts cortos
 * para que una caída de un servicio upstream no cuelgue el perfil del usuario.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(3))
                .build();
    }
}

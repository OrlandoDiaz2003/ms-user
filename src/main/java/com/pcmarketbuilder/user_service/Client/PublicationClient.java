package com.pcmarketbuilder.user_service.Client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pcmarketbuilder.user_service.Dto.PublicationSummary;
import com.pcmarketbuilder.user_service.Dto.SellerPublications;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cliente de solo lectura hacia publication-service (ver Modelo_relacion.sql
 * §3, db_listings). Resuelve, a partir del azure_oid del vendedor (mismo id
 * que sellerId en publications), sus publicaciones para mostrarlas en "mi
 * perfil" (GET /users/me).
 *
 * Deliberadamente NO se usa desde getPublicProfile/getPublicProfileByAzureOid:
 * esos endpoints los llama el BFF una vez por vendedor distinto en cada card
 * del catálogo (ver bff.catalogService.lookupSeller); si esta llamada
 * colgara de ahí, cada búsqueda del catálogo dispararía además N llamadas a
 * publication-service, un fan-out que el BFF ya evita porque él mismo tiene
 * las publicaciones a mano.
 */
@Component
@RequiredArgsConstructor
public class PublicationClient {

    private static final Logger log = LoggerFactory.getLogger(PublicationClient.class);
    private static final int MAX_RESULTS = 50;

    private final RestTemplate restTemplate;

    @Value("${publications.base-url:http://localhost:8083/api/v1/publications}")
    private String publicationsBaseUrl;

    public SellerPublications findBySeller(String sellerAzureOid) {
        String url = UriComponentsBuilder.fromUriString(publicationsBaseUrl)
                .queryParam("sellerId", sellerAzureOid)
                .queryParam("page", 1)
                .queryParam("limit", MAX_RESULTS)
                .toUriString();

        try {
            RemotePage page = restTemplate.getForObject(url, RemotePage.class);
            if (page == null || page.content() == null) {
                return SellerPublications.empty();
            }
            List<PublicationSummary> summaries = page.content().stream()
                    .map(PublicationClient::toSummary)
                    .toList();
            return new SellerPublications((int) page.totalElements(), summaries);
        } catch (RestClientException ex) {
            log.warn("No se pudieron obtener las publicaciones del vendedor {} desde publication-service: {}",
                    sellerAzureOid, ex.getMessage());
            return SellerPublications.empty();
        }
    }

    private static PublicationSummary toSummary(RemotePublication p) {
        return new PublicationSummary(
                p.publicationId(),
                p.title(),
                p.price(),
                p.grade(),
                p.status(),
                primaryImageUrl(p.images()),
                p.createdAt());
    }

    private static String primaryImageUrl(List<RemoteImage> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(RemoteImage::isPrimary)
                .map(RemoteImage::imageUrl)
                .findFirst()
                .orElseGet(() -> images.get(0).imageUrl());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemotePage(List<RemotePublication> content, long totalElements) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemotePublication(
            String publicationId,
            String title,
            Integer price,
            String grade,
            String status,
            LocalDateTime createdAt,
            List<RemoteImage> images) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RemoteImage(String imageId, String imageUrl, boolean isPrimary) {}
}

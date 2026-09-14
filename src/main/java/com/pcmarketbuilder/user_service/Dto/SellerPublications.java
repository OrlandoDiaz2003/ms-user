package com.pcmarketbuilder.user_service.Dto;

import java.util.List;

/**
 * Agregado de las publicaciones de un vendedor: el total real (según
 * publication-service, puede ser mayor a items.size() si hay más de
 * PublicationClient.MAX_RESULTS) y el detalle a mostrar en el perfil.
 */
public record SellerPublications(int totalCount, List<PublicationSummary> items) {
    public static SellerPublications empty() {
        return new SellerPublications(0, List.of());
    }
}

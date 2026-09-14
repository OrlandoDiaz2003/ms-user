package com.pcmarketbuilder.user_service.Dto;

import java.time.LocalDateTime;

/**
 * Resumen de una publicación del vendedor, tal como la expone publication-service.
 * Se usa para mostrar "mis publicaciones" dentro del perfil, sin duplicar el modelo
 * completo de esa base de datos (db_listings).
 */
public record PublicationSummary(
        String publicationId,
        String title,
        Integer price,
        String grade,
        String status,
        String primaryImage,
        LocalDateTime createdAt
) {}

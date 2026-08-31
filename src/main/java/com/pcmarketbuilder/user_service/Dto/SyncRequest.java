package com.pcmarketbuilder.user_service.Dto;

/**
 * Body opcional de POST /users/sync.
 *
 * En producción la identidad y los claims vienen de los headers
 * (X-User-Id / X-User-Role / X-User-Email / X-User-Name) inyectados por el
 * Gateway. Este body solo actúa como fallback en desarrollo para pasar
 * email/full_name cuando no se quieren usar los headers.
 */
public record SyncRequest(
        String email,
        String fullName
) {
}

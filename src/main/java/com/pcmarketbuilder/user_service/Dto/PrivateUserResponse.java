package com.pcmarketbuilder.user_service.Dto;

import com.pcmarketbuilder.user_service.Model.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Perfil privado del usuario autenticado (GET /users/me).
 * Incluye datos sensibles (email, address, full_name) que NO deben exponerse
 * en el perfil público.
 *
 * publicationsCount/publications vienen de publication-service (ver
 * Client.PublicationClient), resueltas en el momento de armar la respuesta:
 * no son columnas de la entidad User.
 */
public record PrivateUserResponse(
        String userId,
        String username,
        String email,
        String fullName,
        String bio,
        String avatarUrl,
        String address,
        String role,
        LocalDateTime createdAt,
        int publicationsCount,
        List<PublicationSummary> publications
) {
    public static PrivateUserResponse fromEntity(User user, SellerPublications publications) {
        return new PrivateUserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getAddress(),
                user.getRole() != null ? user.getRole().getRoleName() : null,
                user.getCreatedAt(),
                publications.totalCount(),
                publications.items()
        );
    }
}

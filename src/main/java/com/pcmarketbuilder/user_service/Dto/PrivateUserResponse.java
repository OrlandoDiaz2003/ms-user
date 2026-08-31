package com.pcmarketbuilder.user_service.Dto;

import com.pcmarketbuilder.user_service.Model.User;

import java.time.LocalDateTime;

/**
 * Perfil privado del usuario autenticado (GET /users/me).
 * Incluye datos sensibles (email, address, full_name) que NO deben exponerse
 * en el perfil público.
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
        LocalDateTime createdAt
) {
    public static PrivateUserResponse fromEntity(User user) {
        return new PrivateUserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getAddress(),
                user.getRole() != null ? user.getRole().getRoleName() : null,
                user.getCreatedAt()
        );
    }
}

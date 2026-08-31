package com.pcmarketbuilder.user_service.Dto;

import com.pcmarketbuilder.user_service.Model.User;

/**
 * Perfil público de un usuario (GET /users/{username}).
 * Excluye datos sensibles: email, address, full_name, role.
 * (La reputación / average_rating queda pendiente — ver doc §5.)
 */
public record PublicUserResponse(
        String username,
        String avatarUrl,
        String bio
) {
    public static PublicUserResponse fromEntity(User user) {
        return new PublicUserResponse(
                user.getUsername(),
                user.getAvatarUrl(),
                user.getBio()
        );
    }
}

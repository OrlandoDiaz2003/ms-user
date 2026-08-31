package com.pcmarketbuilder.user_service.Dto;

import jakarta.validation.constraints.Size;

/**
 * Body de PUT /users/me: actualización del perfil editable por el usuario.
 *
 * Solo bio, avatar_url, address y full_name. NO se permite tocar aquí
 * email, username, azure_oid ni role_id (eso se resuelve vía /sync o
 * administración, ver doc §3).
 */
public record UpdateProfileRequest(

        @Size(max = 500, message = "bio no puede superar 500 caracteres")
        String bio,

        @Size(max = 500, message = "avatarUrl no puede superar 500 caracteres")
        String avatarUrl,

        @Size(max = 255, message = "address no puede superar 255 caracteres")
        String address,

        @Size(max = 255, message = "fullName no puede superar 255 caracteres")
        String fullName
) {
}

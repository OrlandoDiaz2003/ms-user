package com.pcmarketbuilder.user_service.Auth;

import com.pcmarketbuilder.user_service.Exception.ForbiddenException;
import com.pcmarketbuilder.user_service.Exception.UnauthorizedException;

/**
 * Contexto de autenticación/autorización de una request.
 *
 * Aún no hay Gateway/Authorizer real (Azure Entra ID): mientras tanto estos
 * datos se sacan de los headers simulados X-User-Id / X-User-Role, inyectados
 * a mano en tests o curl. Cuando se conecte el proveedor real, solo hay que
 * cambiar cómo se construye este objeto, no la lógica de negocio que lo consume.
 */
public record AuthContext(String userId, String role) {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String ROLE_HEADER = "X-User-Role";
    public static final String EMAIL_HEADER = "X-User-Email";
    public static final String NAME_HEADER = "X-User-Name";

    public static final String ROLE_BUYER_SELLER = "BUYER_SELLER";
    public static final String ROLE_TECHNICAL_AGENT = "TECHNICAL_AGENT";
    public static final String ROLE_WORKSHOP_ADMIN = "WORKSHOP_ADMIN";

    public AuthContext {
        if (userId == null || userId.isBlank()) {
            throw new UnauthorizedException("Falta el encabezado de identidad: " + USER_ID_HEADER);
        }
        if (role == null || role.isBlank()) {
            throw new UnauthorizedException("Falta el encabezado de rol: " + ROLE_HEADER);
        }
    }

    public boolean hasRole(String expectedRole) {
        return role.equals(expectedRole);
    }

    public void requireRole(String expectedRole) {
        if (!hasRole(expectedRole)) {
            throw new ForbiddenException(
                    "Rol '" + expectedRole + "' requerido para esta operación. Rol actual: " + role);
        }
    }

    /** Valida que el usuario autenticado sea el dueño del recurso (userId). */
    public void requireOwner(String userId) {
        if (userId == null || !this.userId.equals(userId)) {
            throw new ForbiddenException(
                    "No tienes permiso para operar sobre este usuario (owner: " + userId + ")");
        }
    }
}

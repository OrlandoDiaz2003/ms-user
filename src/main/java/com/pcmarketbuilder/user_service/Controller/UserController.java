package com.pcmarketbuilder.user_service.Controller;

import com.pcmarketbuilder.user_service.Auth.AuthContext;
import com.pcmarketbuilder.user_service.Dto.PrivateUserResponse;
import com.pcmarketbuilder.user_service.Dto.PublicUserResponse;
import com.pcmarketbuilder.user_service.Dto.SyncRequest;
import com.pcmarketbuilder.user_service.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Provisioning JIT: se llama una vez por sesión, justo después de que el
     * frontend recibe el JWT de Entra. Crea el usuario si no existe o
     * sincroniza email/full_name/rol si cambiaron. Devuelve el perfil privado.
     *
     * En producción la identidad viene de los headers simulados (X-User-*);
     * el body es solo un fallback para email/full_name en desarrollo.
     */
    @PostMapping("/sync")
    public ResponseEntity<PrivateUserResponse> sync(
            @RequestHeader(value = AuthContext.USER_ID_HEADER, required = false) String userId,
            @RequestHeader(value = AuthContext.ROLE_HEADER, required = false) String role,
            @RequestHeader(value = AuthContext.EMAIL_HEADER, required = false) String emailHeader,
            @RequestHeader(value = AuthContext.NAME_HEADER, required = false) String nameHeader,
            @RequestBody(required = false) SyncRequest body) {

        String email = emailHeader != null && !emailHeader.isBlank()
                ? emailHeader
                : (body != null ? body.email() : null);
        String fullName = nameHeader != null && !nameHeader.isBlank()
                ? nameHeader
                : (body != null ? body.fullName() : null);

        AuthContext auth = new AuthContext(userId, role);
        var synced = userService.sync(auth.userId(), auth.role(), email, fullName);
        return ResponseEntity.status(HttpStatus.OK).body(PrivateUserResponse.fromEntity(synced));
    }

    /**
     * Perfil privado completo del usuario autenticado. La identidad sale del
     * header X-User-Id (claim "oid"), nunca de la URL.
     */
    @GetMapping("/me")
    public ResponseEntity<PrivateUserResponse> getMe(
            @RequestHeader(value = AuthContext.USER_ID_HEADER, required = false) String userId,
            @RequestHeader(value = AuthContext.ROLE_HEADER, required = false) String role) {
        return ResponseEntity.ok(
                PrivateUserResponse.fromEntity(userService.getMe(new AuthContext(userId, role))));
    }

    /**
     * Perfil público por username. No requiere autenticación.
     */
    @GetMapping("/{username}")
    public ResponseEntity<PublicUserResponse> getPublicProfile(@PathVariable("username") String username) {
        return ResponseEntity.ok(PublicUserResponse.fromEntity(userService.getPublicProfile(username)));
    }
}

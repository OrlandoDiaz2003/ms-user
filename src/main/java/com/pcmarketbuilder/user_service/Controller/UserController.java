package com.pcmarketbuilder.user_service.Controller;

import com.pcmarketbuilder.user_service.Auth.AuthContext;
import com.pcmarketbuilder.user_service.Client.PublicationClient;
import com.pcmarketbuilder.user_service.Dto.PrivateUserResponse;
import com.pcmarketbuilder.user_service.Dto.PublicUserResponse;
import com.pcmarketbuilder.user_service.Dto.SellerPublications;
import com.pcmarketbuilder.user_service.Dto.SyncRequest;
import com.pcmarketbuilder.user_service.Dto.UpdateProfileRequest;
import com.pcmarketbuilder.user_service.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PublicationClient publicationClient;

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
        // No se consulta publication-service acá: el frontend descarta este body
        // (usa /sync solo para provisionar y dispara un GET /me aparte para leer
        // el perfil), y /sync se llama una vez por sesión, en cada login.
        return ResponseEntity.status(HttpStatus.OK)
                .body(PrivateUserResponse.fromEntity(synced, SellerPublications.empty()));
    }

    /**
     * Actualiza el perfil editable del usuario autenticado (bio, avatarUrl,
     * address, fullName). No permite tocar email/username/azure_oid/role_id.
     */
    @PutMapping("/me")
    public ResponseEntity<PrivateUserResponse> updateMe(
            @RequestHeader(value = AuthContext.USER_ID_HEADER, required = false) String userId,
            @RequestHeader(value = AuthContext.ROLE_HEADER, required = false) String role,
            @Valid @RequestBody UpdateProfileRequest request) {
        AuthContext auth = new AuthContext(userId, role);
        var updated = userService.updateProfile(auth, request);
        var publications = publicationClient.findBySeller(auth.userId());
        return ResponseEntity.ok(PrivateUserResponse.fromEntity(updated, publications));
    }

    /**
     * Perfil privado completo del usuario autenticado. La identidad sale del
     * header X-User-Id (claim "oid"), nunca de la URL.
     */
    @GetMapping("/me")
    public ResponseEntity<PrivateUserResponse> getMe(
            @RequestHeader(value = AuthContext.USER_ID_HEADER, required = false) String userId,
            @RequestHeader(value = AuthContext.ROLE_HEADER, required = false) String role) {
        AuthContext auth = new AuthContext(userId, role);
        var user = userService.getMe(auth);
        var publications = publicationClient.findBySeller(auth.userId());
        return ResponseEntity.ok(PrivateUserResponse.fromEntity(user, publications));
    }

    /**
     * Perfil público por username. No requiere autenticación.
     */
    @GetMapping("/{username}")
    public ResponseEntity<PublicUserResponse> getPublicProfile(@PathVariable("username") String username) {
        return ResponseEntity.ok(PublicUserResponse.fromEntity(userService.getPublicProfile(username)));
    }

    /**
     * Perfil público por azure_oid (mismo shape que GET /{username}). No
     * requiere autenticación. Lo usan otros microservicios/BFFs para resolver
     * un sellerId/buyerId (que almacenan el azure_oid) a un perfil mostrable.
     */
    @GetMapping("/by-id/{azureOid}")
    public ResponseEntity<PublicUserResponse> getPublicProfileByAzureOid(@PathVariable("azureOid") String azureOid) {
        return ResponseEntity.ok(PublicUserResponse.fromEntity(userService.getPublicProfileByAzureOid(azureOid)));
    }
}

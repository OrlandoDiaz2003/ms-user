package com.pcmarketbuilder.user_service.Service;

import com.pcmarketbuilder.user_service.Auth.AuthContext;
import com.pcmarketbuilder.user_service.Dto.UpdateProfileRequest;
import com.pcmarketbuilder.user_service.Exception.UserNotFoundException;
import com.pcmarketbuilder.user_service.Model.Role;
import com.pcmarketbuilder.user_service.Model.User;
import com.pcmarketbuilder.user_service.Repository.RoleRepository;
import com.pcmarketbuilder.user_service.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * Perfil privado del usuario autenticado. El userId del AuthContext es el
     * claim "oid" de Entra (azure_oid), igual que en el flujo de /sync.
     */
    @Transactional(readOnly = true)
    public User getMe(AuthContext auth) {
        return userRepository.findByAzureOid(auth.userId())
                .orElseThrow(() -> UserNotFoundException.byAzureOid(auth.userId()));
    }

    /**
     * Actualiza el perfil editable por el usuario (PUT /users/me): bio,
     * avatar_url, address, full_name. NO toca email, username, azure_oid ni
     * role_id (esos se resuelven vía /sync o administración, ver doc §3).
     *
     * Solo aplica los campos no-nulos; un campo ausente no se sobreescribe.
     */
    @Transactional
    public User updateProfile(AuthContext auth, UpdateProfileRequest request) {
        User user = getMe(auth);

        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        if (request.address() != null) {
            user.setAddress(request.address());
        }
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }

        return userRepository.save(user);
    }

    /**
     * Perfil público por username (no requiere autenticación).
     */
    @Transactional(readOnly = true)
    public User getPublicProfile(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> UserNotFoundException.byUsername(username));
    }

    /**
     * Perfil público por azure_oid (no requiere autenticación). Usado por otros
     * microservicios/BFFs para resolver un sellerId/buyerId (que guardan el
     * azure_oid) a un perfil mostrable.
     */
    @Transactional(readOnly = true)
    public User getPublicProfileByAzureOid(String azureOid) {
        return userRepository.findByAzureOid(azureOid)
                .orElseThrow(() -> UserNotFoundException.byAzureOid(azureOid));
    }

    /**
     * Provisioning JIT (POST /users/sync). Se llama una vez por sesión tras el
     * login exitoso de Entra, con la identidad simulada por headers.
     *
     * Mapeo de claims -> entidad (ver jwt_claims_mapping.txt):
     *   oid                  -> azure_oid (AuthContext.userId)
     *   roles -> roles[0]    -> Role.role_name (AuthContext.role)
     *   preferred_username   -> email
     *   name                 -> full_name
     *
     * @return perfil (existente actualizado, o recién creado)
     */
    @Transactional
    public User sync(String azureOid, String roleName, String email, String fullName) {
        User user = userRepository.findByAzureOid(azureOid).orElse(null);

        if (user == null) {
            return createUser(azureOid, roleName, email, fullName);
        }

        // Usuario existente: sincroniza email/full_name si cambiaron en Entra.
        if (email != null && !email.isBlank() && !email.equals(user.getEmail())) {
            user.setEmail(email);
        }
        if (fullName != null && !fullName.isBlank() && !fullName.equals(user.getFullName())) {
            user.setFullName(fullName);
        }

        // Sincroniza el rol si el claim trae un valor distinto al guardado.
        Role requestedRole = resolveRole(roleName);
        if (requestedRole != null && !requestedRole.getRoleId().equals(user.getRole().getRoleId())) {
            user.setRole(requestedRole);
        }

        return userRepository.save(user);
    }

    private User createUser(String azureOid, String roleName, String email, String fullName) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Falta el email del usuario (claim preferred_username) para el provisioning JIT");
        }

        Role role = resolveRole(roleName);
        if (role == null) {
            role = resolveRole(AuthContext.ROLE_BUYER_SELLER);
        }

        User user = User.builder()
                .azureOid(azureOid)
                .username(generateUsername(email))
                .email(email)
                .fullName(fullName)
                .role(role)
                .build();
        return userRepository.save(user);
    }

    /**
     * Opción A del diseño de username (doc §4): derivar del email (parte antes
     * del '@'), limpiado de caracteres no permitidos, con sufijo numérico si
     * colisiona con uno existente.
     */
    private String generateUsername(String email) {
        String base = email.contains("@") ? email.substring(0, email.indexOf('@')) : "user";
        base = base.replaceAll("[^a-zA-Z0-9._-]", "");
        if (base.isEmpty()) {
            base = "user";
        }

        String candidate = base;
        int suffix = 1;
        while (userRepository.findByUsername(candidate).isPresent()) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }

    /**
     * Resuelve la Role por role_name SOLO leyendo la tabla. Si el valor del
     * claim no existe entre los roles conocidos (sembrados por RoleSeeder),
     * devuelve null: el rol se ignora y se usa el default. NUNCA se crea un
     * rol nuevo a partir del input del cliente (evita auto-inventar permisos
     * mientras no exista el Gateway real validando el JWT).
     */
    private Role resolveRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return null;
        }
        return roleRepository.findByRoleName(roleName).orElse(null);
    }
}

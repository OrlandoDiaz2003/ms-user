package com.pcmarketbuilder.user_service.Config;

import com.pcmarketbuilder.user_service.Auth.AuthContext;
import com.pcmarketbuilder.user_service.Model.Role;
import com.pcmarketbuilder.user_service.Repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Siembra el catálogo de roles conocidos (los App Roles declarados en Entra
 * ID, ver MS_USER_INITIAL_IMPLEMENTATION.md §1) si no existen todavía.
 *
 * Esto permite que /sync resuelva un rol SOLO leyendo la tabla (nunca
 * creándolo): cualquier role_name que no esté en esta lista se ignora y se
 * queda con el default BUYER_SELLER, en vez de auto-inventarse un permiso.
 */
@Component
@RequiredArgsConstructor
public class RoleSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    private static final List<String> KNOWN_ROLES = List.of(
            AuthContext.ROLE_BUYER_SELLER,
            AuthContext.ROLE_TECHNICAL_AGENT,
            AuthContext.ROLE_WORKSHOP_ADMIN
    );

    @Override
    public void run(String... args) {
        for (String roleName : KNOWN_ROLES) {
            if (roleRepository.findByRoleName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().roleName(roleName).build());
            }
        }
    }
}

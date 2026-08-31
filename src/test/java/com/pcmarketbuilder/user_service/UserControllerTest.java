package com.pcmarketbuilder.user_service;

import com.pcmarketbuilder.user_service.Auth.AuthContext;
import com.pcmarketbuilder.user_service.Model.Role;
import com.pcmarketbuilder.user_service.Model.User;
import com.pcmarketbuilder.user_service.Repository.RoleRepository;
import com.pcmarketbuilder.user_service.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    private static final String OID = "2150f48f-e611-439c-83cf-37eed0c5f232";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Siembra los roles conocidos, igual que RoleSeeder en producción.
        Role buyerSeller = roleRepository.save(Role.builder().roleName(AuthContext.ROLE_BUYER_SELLER).build());
        roleRepository.save(Role.builder().roleName(AuthContext.ROLE_TECHNICAL_AGENT).build());
        roleRepository.save(Role.builder().roleName(AuthContext.ROLE_WORKSHOP_ADMIN).build());

        userRepository.save(User.builder()
                .azureOid(OID)
                .username("pansito")
                .email("pansito@pcmarketbuilder.onmicrosoft.com")
                .fullName("pansito")
                .bio("hola")
                .avatarUrl("https://img/avatar.png")
                .address("Av. Siempre Viva")
                .role(buyerSeller)
                .build());
    }

    @Test
    void getMeReturnsPrivateProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header(AuthContext.USER_ID_HEADER, OID)
                        .header(AuthContext.ROLE_HEADER, AuthContext.ROLE_BUYER_SELLER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("pansito")))
                .andExpect(jsonPath("$.email", is("pansito@pcmarketbuilder.onmicrosoft.com")))
                .andExpect(jsonPath("$.address", is("Av. Siempre Viva")))
                .andExpect(jsonPath("$.fullName", is("pansito")))
                .andExpect(jsonPath("$.role", is("BUYER_SELLER")))
                .andExpect(jsonPath("$.userId", is(containsString("-"))));
    }

    @Test
    void getMeWithoutHeadersReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPublicProfileReturnsOnlyPublicFields() throws Exception {
        mockMvc.perform(get("/api/v1/users/pansito"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("pansito")))
                .andExpect(jsonPath("$.avatarUrl", is("https://img/avatar.png")))
                .andExpect(jsonPath("$.bio", is("hola")))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasKey("email"))))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasKey("address"))))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasKey("fullName"))));
    }

    @Test
    void getPublicProfileNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/users/noexiste"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPublicProfileByAzureOidReturnsOnlyPublicFields() throws Exception {
        mockMvc.perform(get("/api/v1/users/by-id/" + OID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("pansito")))
                .andExpect(jsonPath("$.avatarUrl", is("https://img/avatar.png")))
                .andExpect(jsonPath("$.bio", is("hola")))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasKey("email"))))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasKey("address"))))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasKey("userId"))));
    }

    @Test
    void getPublicProfileByAzureOidNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/users/by-id/no-existe-oid"))
                .andExpect(status().isNotFound());
    }

    @Test
    void syncCreatesNewUserFromHeaders() throws Exception {
        mockMvc.perform(post("/api/v1/users/sync")
                        .header(AuthContext.USER_ID_HEADER, "99999999-0000-0000-0000-000000000001")
                        .header(AuthContext.ROLE_HEADER, AuthContext.ROLE_BUYER_SELLER)
                        .header(AuthContext.EMAIL_HEADER, "nuevo@pcmarketbuilder.onmicrosoft.com")
                        .header(AuthContext.NAME_HEADER, "Nuevo Usuario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("nuevo@pcmarketbuilder.onmicrosoft.com")))
                .andExpect(jsonPath("$.fullName", is("Nuevo Usuario")))
                .andExpect(jsonPath("$.username", is("nuevo")))
                .andExpect(jsonPath("$.role", is("BUYER_SELLER")));

        // El perfil privado ahora existe: /me debe resolverlo por azure_oid.
        mockMvc.perform(get("/api/v1/users/me")
                        .header(AuthContext.USER_ID_HEADER, "99999999-0000-0000-0000-000000000001")
                        .header(AuthContext.ROLE_HEADER, AuthContext.ROLE_BUYER_SELLER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("nuevo")));
    }

    @Test
    void syncCreatesUsernameWithSuffixOnCollision() throws Exception {
        // Ya existe "pansito" (creado en setUp). Un nuevo usuario con mismo
        // prefijo de email debe colisionar y derivar "pansito1".
        mockMvc.perform(post("/api/v1/users/sync")
                        .header(AuthContext.USER_ID_HEADER, "99999999-0000-0000-0000-000000000002")
                        .header(AuthContext.ROLE_HEADER, AuthContext.ROLE_BUYER_SELLER)
                        .header(AuthContext.EMAIL_HEADER, "pansito@otrodominio.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("pansito1")))
                .andExpect(jsonPath("$.email", is("pansito@otrodominio.com")));
    }

    @Test
    void syncUpdatesExistingUserEmailAndName() throws Exception {
        // Actualiza el usuario existente (azure_oid OID) con nuevo email y nombre.
        mockMvc.perform(post("/api/v1/users/sync")
                        .header(AuthContext.USER_ID_HEADER, OID)
                        .header(AuthContext.ROLE_HEADER, AuthContext.ROLE_BUYER_SELLER)
                        .header(AuthContext.EMAIL_HEADER, "pansito.nuevo@pcmarketbuilder.onmicrosoft.com")
                        .header(AuthContext.NAME_HEADER, "Pansito Actualizado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("pansito.nuevo@pcmarketbuilder.onmicrosoft.com")))
                .andExpect(jsonPath("$.fullName", is("Pansito Actualizado")))
                .andExpect(jsonPath("$.username", is("pansito"))); // username no cambia
    }

    @Test
    void syncAssignsKnownRoleToNewUser() throws Exception {
        mockMvc.perform(post("/api/v1/users/sync")
                        .header(AuthContext.USER_ID_HEADER, "99999999-0000-0000-0000-000000000003")
                        .header(AuthContext.ROLE_HEADER, "WORKSHOP_ADMIN")
                        .header(AuthContext.EMAIL_HEADER, "admin@pcmarketbuilder.onmicrosoft.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("WORKSHOP_ADMIN")));
    }

    @Test
    void syncIgnoresUnknownRoleOnNewUserAndDefaultsToBuyerSeller() throws Exception {
        // Rol inventado: no está entre los conocidos, se ignora -> default BUYER_SELLER.
        mockMvc.perform(post("/api/v1/users/sync")
                        .header(AuthContext.USER_ID_HEADER, "99999999-0000-0000-0000-000000000005")
                        .header(AuthContext.ROLE_HEADER, "SUPER_ADMIN")
                        .header(AuthContext.EMAIL_HEADER, "desconocido@pcmarketbuilder.onmicrosoft.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("BUYER_SELLER")));

        // El rol inventado no debe haberse creado en la tabla roles.
        assertThat(roleRepository.findByRoleName("SUPER_ADMIN")).isEmpty();
    }

    @Test
    void syncWithUnknownRoleKeepsExistingRole() throws Exception {
        // pansito ya tiene BUYER_SELLER. Mandar un rol mal escrito no debe cambiarlo.
        mockMvc.perform(post("/api/v1/users/sync")
                        .header(AuthContext.USER_ID_HEADER, OID)
                        .header(AuthContext.ROLE_HEADER, "WORKSHOP_ADMIM") // typo intencional
                        .header(AuthContext.EMAIL_HEADER, "pansito@pcmarketbuilder.onmicrosoft.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("BUYER_SELLER")));

        assertThat(roleRepository.findByRoleName("WORKSHOP_ADMIM")).isEmpty();
    }

    @Test
    void syncWithoutEmailInCreationPathReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/users/sync")
                        .header(AuthContext.USER_ID_HEADER, "99999999-0000-0000-0000-000000000004")
                        .header(AuthContext.ROLE_HEADER, AuthContext.ROLE_BUYER_SELLER))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMeChangesEditableProfileFields() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .header(AuthContext.USER_ID_HEADER, OID)
                        .header(AuthContext.ROLE_HEADER, AuthContext.ROLE_BUYER_SELLER)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "bio": "Vendedor de GPUs usadas",
                                  "avatarUrl": "https://img/avatar-nuevo.png",
                                  "address": "Av. Nueva 123",
                                  "fullName": "Pansito Pérez"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio", is("Vendedor de GPUs usadas")))
                .andExpect(jsonPath("$.avatarUrl", is("https://img/avatar-nuevo.png")))
                .andExpect(jsonPath("$.address", is("Av. Nueva 123")))
                .andExpect(jsonPath("$.fullName", is("Pansito Pérez")))
                // Campos no editables quedan intactos:
                .andExpect(jsonPath("$.email", is("pansito@pcmarketbuilder.onmicrosoft.com")))
                .andExpect(jsonPath("$.username", is("pansito")));
    }

    @Test
    void updateMeWithPartialBodyOnlyChangesProvidedFields() throws Exception {
        // Solo manda bio; el resto del perfil no debe sobreescribirse.
        mockMvc.perform(put("/api/v1/users/me")
                        .header(AuthContext.USER_ID_HEADER, OID)
                        .header(AuthContext.ROLE_HEADER, AuthContext.ROLE_BUYER_SELLER)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "bio": "solo cambio el bio" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio", is("solo cambio el bio")))
                .andExpect(jsonPath("$.avatarUrl", is("https://img/avatar.png")))
                .andExpect(jsonPath("$.fullName", is("pansito")));
    }

    @Test
    void updateMeRejectsTooLongField() throws Exception {
        String longBio = "a".repeat(501);
        mockMvc.perform(put("/api/v1/users/me")
                        .header(AuthContext.USER_ID_HEADER, OID)
                        .header(AuthContext.ROLE_HEADER, AuthContext.ROLE_BUYER_SELLER)
                        .contentType(APPLICATION_JSON)
                        .content("{ \"bio\": \"" + longBio + "\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON));
    }

    @Test
    void updateMeWithoutHeadersReturnsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "bio": "hola" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateMeForUnknownUserReturnsNotFound() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .header(AuthContext.USER_ID_HEADER, "no-existe-oid-999")
                        .header(AuthContext.ROLE_HEADER, AuthContext.ROLE_BUYER_SELLER)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                { "bio": "hola" }
                                """))
                .andExpect(status().isNotFound());
    }
}

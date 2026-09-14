package com.pcmarketbuilder.user_service;

import com.pcmarketbuilder.user_service.Model.Role;
import com.pcmarketbuilder.user_service.Model.User;
import com.pcmarketbuilder.user_service.Repository.RoleRepository;
import com.pcmarketbuilder.user_service.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserModelTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createsRoleAndUserWithRelationship() {
        // Usa un role_name que RoleSeeder ya no siembra para evitar colisión UNIQUE.
        Role role = roleRepository.save(Role.builder().roleName("").build());

        User user = userRepository.save(User.builder()
                .azureOid("2150f48f-e611-439c-83cf-37eed0c5f232")
                .username("pansito")
                .email("pansito@pcmarketbuilder.onmicrosoft.com")
                .fullName("pansito")
                .role(role)
                .build());

        User loaded = userRepository.findById(user.getUserId()).orElseThrow();

        assertThat(loaded.getUserId()).isNotNull();
        assertThat(loaded.getAzureOid()).isEqualTo("2150f48f-e611-439c-83cf-37eed0c5f232");
        assertThat(loaded.getUsername()).isEqualTo("pansito");
        assertThat(loaded.getEmail()).isEqualTo("pansito@pcmarketbuilder.onmicrosoft.com");
        assertThat(loaded.getRole().getRoleName()).isEqualTo("TEST_ROLE");
        assertThat(loaded.getCreatedAt()).isNotNull();
    }
}

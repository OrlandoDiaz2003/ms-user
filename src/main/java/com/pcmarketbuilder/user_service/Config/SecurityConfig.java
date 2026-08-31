package com.pcmarketbuilder.user_service.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * La autenticación/authorización real ocurre en el API Gateway (Azure Entra ID)
 * que inyecta la identidad como headers X-User-Id / X-User-Role. Por eso aquí
 * se desactiva el login/form y Basic por defecto de Spring Security y se dejan
 * pasar todas las requests: el control del acceso se hace contra esos headers.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}

package com.pcmarketbuilder.user_service.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * Entidad de la tabla "users" (base de datos db_users).
 * Ver MS_USER_INITIAL_IMPLEMENTATION.md, sección 2.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @UuidGenerator
    @Column(name = "user_id", columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private String userId;

    // claim "oid" del JWT de Azure Entra ID.
    @Column(name = "azure_oid", nullable = false, unique = true)
    private String azureOid;

    // Identidad pública principal; se muestra en perfil, reviews y listados.
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    // claim "preferred_username" (no viene claim "email"/"emails" en este tenant).
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    // claim "name" de Entra, opcional.
    @Column(name = "full_name")
    private String fullName;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "address")
    private String address;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

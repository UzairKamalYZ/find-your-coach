package com.findmycoach.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = @Index(name = "ix_users_role", columnList = "role"))
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Always stored lower-cased. */
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    /** BCrypt hash; null for demo social accounts that never set a password. */
    @Column(length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String city;

    @Column(length = 40)
    private String phone;

    @Column(length = 500)
    private String avatar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.TRAINEE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider = AuthProvider.EMAIL;

    @Column(length = 1000)
    private String bio;

    @Column(length = 200)
    private String sports;

    @Column(length = 300)
    private String goals;

    @Column(length = 40)
    private String level;

    @Column(length = 10)
    private String experience;

    @Column(length = 300)
    private String qualifications;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant lastLoginAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}

package com.findmycoach.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Append-only record of administrative and security-relevant actions. */
@Entity
@Table(name = "audit_logs", indexes = @Index(name = "ix_audit_created", columnList = "createdAt"))
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID actorId;

    @Column(length = 254)
    private String actorEmail;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(length = 40)
    private String targetType;

    @Column(length = 80)
    private String targetId;

    @Column(length = 1000)
    private String details;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

package com.findmycoach.notification;

import com.findmycoach.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = @Index(name = "ix_notifications_user", columnList = "user_id,read_flag"))
@Getter
@Setter
@NoArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 500)
    private String message;

    /** Frontend route to open, e.g. "/my-calendar". */
    @Column(length = 200)
    private String link;

    @Column(name = "read_flag", nullable = false)
    private boolean read;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

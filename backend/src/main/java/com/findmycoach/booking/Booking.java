package com.findmycoach.booking;

import com.findmycoach.trainer.Trainer;
import com.findmycoach.user.Role;
import com.findmycoach.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "ix_bookings_trainer_day", columnList = "trainer_id,booking_date"),
        @Index(name = "ix_bookings_trainee", columnList = "trainee_id"),
        @Index(name = "ix_bookings_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainee_id", nullable = false)
    private User trainee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id", nullable = false)
    private Trainer trainer;

    /** Snapshots taken at booking time so history stays accurate if the listing changes. */
    @Column(nullable = false, length = 100)
    private String trainerName;

    @Column(length = 500)
    private String trainerImage;

    @Column(nullable = false, length = 50)
    private String sport;

    @Column(name = "booking_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 5)
    private String slot;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Role cancelledBy;

    @Column(length = 300)
    private String cancellationReason;

    private Instant cancelledAt;

    @Column(nullable = false)
    private boolean lateCancellation;

    @Column(nullable = false)
    private int rescheduleCount;

    @Version
    private long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

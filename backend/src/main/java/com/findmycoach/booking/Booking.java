package com.findmycoach.booking;

import com.findmycoach.trainer.Trainer;
import com.findmycoach.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter @Setter @NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id", nullable = false)
    private Trainer trainer;

    @Column(nullable = false)
    private String trainerName;

    private String trainerImageUrl;

    @Column(nullable = false)
    private String sport;

    @Column(nullable = false)
    private LocalDate bookingDate;

    @Column(nullable = false)
    private String slotTime;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private String status = "CONFIRMED";

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

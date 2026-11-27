package com.findmycoach.trainer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/** A whole day on which a coach is unavailable (holiday, tournament, illness...). */
@Entity
@Table(name = "trainer_time_off", uniqueConstraints = @UniqueConstraint(name = "uq_time_off_day", columnNames = {"trainer_id", "off_date"}))
@Getter
@Setter
@NoArgsConstructor
public class TimeOff {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainer_id", nullable = false)
    private Trainer trainer;

    @Column(name = "off_date", nullable = false)
    private LocalDate date;

    @Column(length = 200)
    private String reason;
}

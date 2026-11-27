package com.findmycoach.venue;

import com.findmycoach.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "venue_bookings", indexes = {
        @Index(name = "ix_vb_venue_day", columnList = "venue_id,booking_date"),
        @Index(name = "ix_vb_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class VenueBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "booking_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime startTime;

    /** Consecutive hours booked (1-4). */
    @Column(nullable = false)
    private int hours;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VenueBookingStatus status = VenueBookingStatus.CONFIRMED;

    @Column(length = 500)
    private String notes;

    @Column(length = 300)
    private String cancellationReason;

    private Instant cancelledAt;

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

    public LocalTime endTime() {
        return startTime.plusHours(hours);
    }

    public boolean overlaps(LocalTime otherStart, int otherHours) {
        LocalTime otherEnd = otherStart.plusHours(otherHours);
        return startTime.isBefore(otherEnd) && otherStart.isBefore(endTime());
    }
}

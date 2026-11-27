package com.findmycoach.trainer;

import com.findmycoach.common.jpa.StringListConverter;
import com.findmycoach.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "trainers", indexes = {
        @Index(name = "ix_trainers_sport", columnList = "sport"),
        @Index(name = "ix_trainers_city", columnList = "city"),
        @Index(name = "ix_trainers_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class Trainer {

    /** URL slug, e.g. "marcus-reed". */
    @Id
    @Column(length = 80)
    private String id;

    /** The coach's user account; null for listings managed only by admins. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String sport;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 1000)
    private String bio;

    /** Either a frontend asset key (e.g. "trainer-1") or an absolute image URL. */
    @Column(length = 500)
    private String image;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal hourlyRate;

    @Column(nullable = false)
    private int experience;

    @Convert(converter = StringListConverter.class)
    @Column(length = 1000)
    private List<String> specialties = new ArrayList<>();

    @Column(length = 300)
    private String qualifications;

    /** Sum of all ratings, including the imported historical baseline. */
    @Column(nullable = false)
    private double ratingTotal;

    @Column(nullable = false)
    private int reviewCount;

    /** Cached average (one decimal) for sorting/filtering. */
    @Column(nullable = false)
    private double rating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrainerStatus status = TrainerStatus.PENDING;

    @Column(length = 300)
    private String statusReason;

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false)
    private int sessionMinutes = 60;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "trainer_weekly_slots", joinColumns = @JoinColumn(name = "trainer_id"))
    private Set<WeeklySlot> weeklySlots = new HashSet<>();

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

    public void addRating(int stars) {
        ratingTotal += stars;
        reviewCount += 1;
        recalcRating();
    }

    public void removeRating(int stars) {
        if (reviewCount <= 0) return;
        ratingTotal = Math.max(0, ratingTotal - stars);
        reviewCount -= 1;
        recalcRating();
    }

    public void recalcRating() {
        rating = reviewCount == 0 ? 0 : BigDecimal.valueOf(ratingTotal / reviewCount)
                .setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    public boolean isBookable() {
        return status == TrainerStatus.ACTIVE;
    }
}

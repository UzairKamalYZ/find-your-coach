package com.findmycoach.venue;

import com.findmycoach.common.jpa.StringListConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "venues", indexes = {
        @Index(name = "ix_venues_sport", columnList = "sport"),
        @Index(name = "ix_venues_city", columnList = "city")
})
@Getter
@Setter
@NoArgsConstructor
public class Venue {
    @Id
    @Column(length = 80)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VenueType type;

    @Column(nullable = false, length = 50)
    private String sport;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 200)
    private String address;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal pricePerHour;

    @Column(nullable = false)
    private double rating;

    /** Frontend asset key (e.g. "venue-tennis") or absolute URL. */
    @Column(length = 500)
    private String image;

    @Convert(converter = StringListConverter.class)
    @Column(length = 1000)
    private List<String> amenities = new ArrayList<>();

    /** Number of courts/pitches that can be booked in the same hour. */
    @Column(nullable = false)
    private int capacity = 1;

    @Column(nullable = false)
    private LocalTime openTime = LocalTime.of(7, 0);

    @Column(nullable = false)
    private LocalTime closeTime = LocalTime.of(22, 0);

    @Column(nullable = false)
    private boolean active = true;

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

package com.findmycoach.venue;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "venues")
@Getter @Setter @NoArgsConstructor
public class Venue {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String sport;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private int pricePerHour;

    @Column(nullable = false)
    private BigDecimal rating;

    private String imageUrl;
}

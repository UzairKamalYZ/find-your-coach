package com.findmycoach.trainer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "trainers")
@Getter @Setter @NoArgsConstructor
public class Trainer {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sport;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private BigDecimal rating;

    @Column(nullable = false)
    private int reviews;

    @Column(nullable = false)
    private int hourlyRate;

    @Column(nullable = false)
    private int experience;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String specialties;
}

package com.findmycoach.sport;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sports")
@Getter
@Setter
@NoArgsConstructor
public class Sport {
    @Id
    @Column(length = 50)
    private String name;

    @Column(length = 16)
    private String emoji;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private int sortOrder;

    public Sport(String name, String emoji, int sortOrder) {
        this.name = name;
        this.emoji = emoji;
        this.sortOrder = sortOrder;
    }
}

package com.findmycoach.trainer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TimeOffRepository extends JpaRepository<TimeOff, UUID> {
    List<TimeOff> findByTrainerIdAndDateBetweenOrderByDateAsc(String trainerId, LocalDate from, LocalDate to);

    List<TimeOff> findByTrainerIdAndDateGreaterThanEqualOrderByDateAsc(String trainerId, LocalDate from);

    boolean existsByTrainerIdAndDate(String trainerId, LocalDate date);
}

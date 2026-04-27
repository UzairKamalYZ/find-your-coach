package com.findmycoach.trainer;

import com.findmycoach.booking.Booking;
import com.findmycoach.booking.BookingRepository;
import com.findmycoach.common.exception.AppException;
import com.findmycoach.trainer.dto.DayAvailability;
import com.findmycoach.trainer.dto.TrainerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainerService {

    private static final List<String> ALL_SLOTS =
        List.of("08:00", "09:00", "10:00", "12:00", "14:00", "15:00", "17:00", "18:00", "19:00");

    private final TrainerRepository trainerRepository;
    private final BookingRepository bookingRepository;

    public Page<TrainerResponse> search(String sport, String city, String q, Pageable pageable) {
        return trainerRepository.search(
            (sport != null && !sport.isBlank()) ? sport : null,
            (city != null && !city.isBlank()) ? city : null,
            (q != null && !q.isBlank()) ? q : null,
            pageable
        ).map(TrainerResponse::from);
    }

    public TrainerResponse getById(String id) {
        return trainerRepository.findById(id)
            .map(TrainerResponse::from)
            .orElseThrow(() -> AppException.notFound("Trainer not found"));
    }

    public List<DayAvailability> getAvailability(String trainerId, LocalDate from, LocalDate to) {
        if (ChronoUnit.DAYS.between(from, to) > 14) {
            throw AppException.badRequest("Date range cannot exceed 14 days");
        }
        trainerRepository.findById(trainerId)
            .orElseThrow(() -> AppException.notFound("Trainer not found"));

        List<Booking> booked = bookingRepository
            .findByTrainerIdAndBookingDateBetweenAndStatus(trainerId, from, to, "CONFIRMED");

        Set<String> bookedKeys = booked.stream()
            .map(b -> b.getBookingDate() + ":" + b.getSlotTime())
            .collect(Collectors.toSet());

        List<DayAvailability> result = new ArrayList<>();
        LocalDate current = from;
        while (!current.isAfter(to)) {
            final LocalDate day = current;
            List<String> available = ALL_SLOTS.stream()
                .filter(slot -> !bookedKeys.contains(day + ":" + slot))
                .toList();
            result.add(new DayAvailability(day.toString(), available));
            current = current.plusDays(1);
        }
        return result;
    }
}

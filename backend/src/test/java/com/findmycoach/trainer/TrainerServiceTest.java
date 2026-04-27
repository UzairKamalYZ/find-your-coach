package com.findmycoach.trainer;

import com.findmycoach.booking.Booking;
import com.findmycoach.booking.BookingRepository;
import com.findmycoach.common.exception.AppException;
import com.findmycoach.trainer.dto.DayAvailability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TrainerServiceTest {

    private TrainerRepository trainerRepository;
    private BookingRepository bookingRepository;
    private TrainerService trainerService;

    @BeforeEach
    void setUp() {
        trainerRepository = mock(TrainerRepository.class);
        bookingRepository = mock(BookingRepository.class);
        trainerService = new TrainerService(trainerRepository, bookingRepository);
    }

    @Test
    void getAvailability_rangeExceeds14Days_throwsBadRequest() {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(30);
        assertThatThrownBy(() -> trainerService.getAvailability("t1", from, to))
            .isInstanceOf(AppException.class)
            .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getAvailability_trainerNotFound_throwsNotFound() {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(3);
        when(trainerRepository.findById("t1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> trainerService.getAvailability("t1", from, to))
            .isInstanceOf(AppException.class)
            .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getAvailability_excludesBookedSlots() {
        LocalDate date = LocalDate.now();
        Trainer trainer = new Trainer();
        trainer.setId("t1");
        when(trainerRepository.findById("t1")).thenReturn(Optional.of(trainer));

        Booking booking = new Booking();
        booking.setBookingDate(date);
        booking.setSlotTime("08:00");
        when(bookingRepository.findByTrainerIdAndBookingDateBetweenAndStatus(
            "t1", date, date, "CONFIRMED")).thenReturn(List.of(booking));

        List<DayAvailability> result = trainerService.getAvailability("t1", date, date);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).slots()).doesNotContain("08:00");
        assertThat(result.get(0).slots()).contains("09:00");
    }
}

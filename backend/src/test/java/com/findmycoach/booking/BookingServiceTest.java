package com.findmycoach.booking;

import com.findmycoach.booking.dto.CreateBookingRequest;
import com.findmycoach.common.exception.AppException;
import com.findmycoach.trainer.Trainer;
import com.findmycoach.trainer.TrainerRepository;
import com.findmycoach.user.User;
import com.findmycoach.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookingServiceTest {

    private BookingRepository bookingRepository;
    private TrainerRepository trainerRepository;
    private UserRepository userRepository;
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        trainerRepository = mock(TrainerRepository.class);
        userRepository = mock(UserRepository.class);
        bookingService = new BookingService(bookingRepository, trainerRepository, userRepository);
    }

    @Test
    void createBooking_slotAlreadyTaken_throwsConflict() {
        String userId = UUID.randomUUID().toString();
        User user = new User(); user.setId(UUID.fromString(userId));
        Trainer trainer = trainer("t1", 6500);
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(trainerRepository.findById("t1")).thenReturn(Optional.of(trainer));
        when(bookingRepository.existsByTrainerIdAndBookingDateAndSlotTimeAndStatus(
            "t1", LocalDate.now(), "08:00", "CONFIRMED")).thenReturn(true);

        assertThatThrownBy(() -> bookingService.create(userId,
            new CreateBookingRequest("t1", LocalDate.now(), "08:00")))
            .isInstanceOf(AppException.class)
            .extracting("status").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createBooking_snapshotsHourlyRateAsPrice() {
        String userId = UUID.randomUUID().toString();
        User user = new User(); user.setId(UUID.fromString(userId));
        Trainer trainer = trainer("t1", 6500);
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(trainerRepository.findById("t1")).thenReturn(Optional.of(trainer));
        when(bookingRepository.existsByTrainerIdAndBookingDateAndSlotTimeAndStatus(
            any(), any(), any(), any())).thenReturn(false);
        when(bookingRepository.save(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(UUID.randomUUID());
            return b;
        });

        bookingService.create(userId, new CreateBookingRequest("t1", LocalDate.now(), "09:00"));

        verify(bookingRepository).save(argThat(b -> b.getPrice() == 6500));
    }

    @Test
    void cancelBooking_pastBooking_throwsUnprocessable() {
        Booking booking = new Booking();
        booking.setBookingDate(LocalDate.now().minusDays(1));
        String ownerId = UUID.randomUUID().toString();
        User owner = new User(); owner.setId(UUID.fromString(ownerId));
        booking.setUser(owner);
        booking.setStatus("CONFIRMED");
        when(bookingRepository.findById(any())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(UUID.randomUUID().toString(), ownerId))
            .isInstanceOf(AppException.class)
            .extracting("status").isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void cancelBooking_notOwner_throwsForbidden() {
        Booking booking = new Booking();
        booking.setBookingDate(LocalDate.now().plusDays(1));
        User owner = new User(); owner.setId(UUID.randomUUID());
        booking.setUser(owner);
        booking.setStatus("CONFIRMED");
        when(bookingRepository.findById(any())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
            bookingService.cancel(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
            .isInstanceOf(AppException.class)
            .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
    }

    private Trainer trainer(String id, int hourlyRate) {
        Trainer t = new Trainer();
        t.setId(id); t.setName("Coach"); t.setSport("Tennis");
        t.setCity("London"); t.setRating(BigDecimal.valueOf(4.5));
        t.setHourlyRate(hourlyRate); t.setSpecialties("[]");
        return t;
    }
}

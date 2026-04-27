package com.findmycoach.booking;

import com.findmycoach.booking.dto.BookingResponse;
import com.findmycoach.booking.dto.CreateBookingRequest;
import com.findmycoach.common.exception.AppException;
import com.findmycoach.trainer.Trainer;
import com.findmycoach.trainer.TrainerRepository;
import com.findmycoach.user.User;
import com.findmycoach.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TrainerRepository trainerRepository;
    private final UserRepository userRepository;

    @Transactional
    public BookingResponse create(String userId, CreateBookingRequest request) {
        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow();
        Trainer trainer = trainerRepository.findById(request.trainerId())
            .orElseThrow(() -> AppException.notFound("Trainer not found"));

        boolean exists = bookingRepository.existsByTrainerIdAndBookingDateAndSlotTimeAndStatus(
            request.trainerId(), request.date(), request.slot(), "CONFIRMED");
        if (exists) throw AppException.conflict("Slot already booked");

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setTrainer(trainer);
        booking.setTrainerName(trainer.getName());
        booking.setTrainerImageUrl(trainer.getImageUrl());
        booking.setSport(trainer.getSport());
        booking.setBookingDate(request.date());
        booking.setSlotTime(request.slot());
        booking.setPrice(trainer.getHourlyRate());

        try {
            return BookingResponse.from(bookingRepository.save(booking));
        } catch (DataIntegrityViolationException e) {
            throw AppException.conflict("Slot already booked");
        }
    }

    public List<BookingResponse> list(String userId, String status) {
        User user = userRepository.findById(UUID.fromString(userId)).orElseThrow();
        LocalDate today = LocalDate.now();
        return switch (status) {
            case "upcoming" -> bookingRepository
                .findByUserAndBookingDateGreaterThanEqualOrderByBookingDateAscSlotTimeAsc(user, today)
                .stream().map(BookingResponse::from).toList();
            case "past" -> bookingRepository
                .findByUserAndBookingDateBeforeOrderByBookingDateAscSlotTimeAsc(user, today)
                .stream().map(BookingResponse::from).toList();
            default -> bookingRepository
                .findByUserOrderByBookingDateAscSlotTimeAsc(user)
                .stream().map(BookingResponse::from).toList();
        };
    }

    @Transactional
    public void cancel(String bookingId, String userId) {
        Booking booking = bookingRepository.findById(UUID.fromString(bookingId))
            .orElseThrow(() -> AppException.notFound("Booking not found"));

        if (!booking.getUser().getId().toString().equals(userId)) {
            throw AppException.forbidden("Not your booking");
        }
        if (booking.getBookingDate().isBefore(LocalDate.now())) {
            throw AppException.unprocessable("Cannot cancel a past booking");
        }
        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
    }
}

package com.findmycoach.booking;

import com.findmycoach.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserOrderByBookingDateAscSlotTimeAsc(User user);

    List<Booking> findByUserAndBookingDateGreaterThanEqualOrderByBookingDateAscSlotTimeAsc(
        User user, LocalDate from);

    List<Booking> findByUserAndBookingDateBeforeOrderByBookingDateAscSlotTimeAsc(
        User user, LocalDate before);

    boolean existsByTrainerIdAndBookingDateAndSlotTimeAndStatus(
        String trainerId, LocalDate date, String slotTime, String status);

    List<Booking> findByTrainerIdAndBookingDateBetweenAndStatus(
        String trainerId, LocalDate from, LocalDate to, String status);
}

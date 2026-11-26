package com.findmycoach.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @EntityGraph(attributePaths = {"trainer", "trainee"})
    List<Booking> findByTraineeIdOrderByDateAscSlotAsc(UUID traineeId);

    @EntityGraph(attributePaths = {"trainer", "trainee"})
    List<Booking> findByTrainerIdAndDateBetweenOrderByDateAscSlotAsc(String trainerId, LocalDate from, LocalDate to);

    List<Booking> findByTrainerIdAndDateBetweenAndStatusIn(String trainerId, LocalDate from, LocalDate to,
                                                           Collection<BookingStatus> statuses);

    boolean existsByTrainerIdAndDateAndSlotAndStatusIn(String trainerId, LocalDate date, String slot,
                                                       Collection<BookingStatus> statuses);

    boolean existsByTraineeIdAndDateAndSlotAndStatus(UUID traineeId, LocalDate date, String slot, BookingStatus status);

    @EntityGraph(attributePaths = {"trainer", "trainee"})
    @Query("""
            select b from Booking b
            where b.trainer.id = :trainerId
              and (:status is null or b.status = :status)
              and (:from is null or b.date >= :from)
              and (:to is null or b.date <= :to)
            """)
    Page<Booking> searchForTrainer(@Param("trainerId") String trainerId, @Param("status") BookingStatus status,
                                   @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    @EntityGraph(attributePaths = {"trainer", "trainee"})
    @Query("""
            select b from Booking b
            where (:trainerId is null or b.trainer.id = :trainerId)
              and (:traineeId is null or b.trainee.id = :traineeId)
              and (:status is null or b.status = :status)
              and (:from is null or b.date >= :from)
              and (:to is null or b.date <= :to)
            """)
    Page<Booking> search(@Param("trainerId") String trainerId, @Param("traineeId") UUID traineeId,
                         @Param("status") BookingStatus status, @Param("from") LocalDate from,
                         @Param("to") LocalDate to, Pageable pageable);

    long countByStatus(BookingStatus status);

    long countByTrainerIdAndStatus(String trainerId, BookingStatus status);

    @Query("select count(distinct b.trainee.id) from Booking b where b.trainer.id = :trainerId and b.status <> com.findmycoach.booking.BookingStatus.CANCELLED")
    long countDistinctTrainees(@Param("trainerId") String trainerId);

    @Query("select coalesce(sum(b.price), 0) from Booking b where b.status = com.findmycoach.booking.BookingStatus.COMPLETED")
    BigDecimal completedRevenue();

    @Query("select coalesce(sum(b.price), 0) from Booking b where b.status = com.findmycoach.booking.BookingStatus.CONFIRMED")
    BigDecimal pipelineRevenue();

    @Query("select coalesce(sum(b.price), 0) from Booking b where b.trainer.id = :trainerId and b.status = :status")
    BigDecimal sumPriceForTrainer(@Param("trainerId") String trainerId, @Param("status") BookingStatus status);

    @Query("select coalesce(sum(b.price), 0) from Booking b where b.trainer.id = :trainerId and b.status = com.findmycoach.booking.BookingStatus.COMPLETED and b.date between :from and :to")
    BigDecimal completedEarningsBetween(@Param("trainerId") String trainerId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select count(b) from Booking b where b.createdAt >= :since")
    long countCreatedSince(@Param("since") java.time.Instant since);

    @Query("""
            select b.trainer.id, b.trainerName, count(b), coalesce(sum(b.price), 0) from Booking b
            where b.status <> com.findmycoach.booking.BookingStatus.CANCELLED
            group by b.trainer.id, b.trainerName order by count(b) desc
            """)
    List<Object[]> topTrainers(Pageable pageable);

    @Query("select b.sport, count(b) from Booking b where b.status <> com.findmycoach.booking.BookingStatus.CANCELLED group by b.sport order by count(b) desc")
    List<Object[]> countBySport();

    @Query("select b.date, count(b) from Booking b where b.date between :from and :to and b.status <> com.findmycoach.booking.BookingStatus.CANCELLED group by b.date order by b.date")
    List<Object[]> countPerDay(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select b from Booking b where b.status = com.findmycoach.booking.BookingStatus.CONFIRMED and b.date < :today")
    List<Booking> findConfirmedBefore(@Param("today") LocalDate today);

    @Query("select b from Booking b where b.trainee.id = :userId and b.status = com.findmycoach.booking.BookingStatus.CONFIRMED and b.date >= :from")
    List<Booking> findActiveFutureForTrainee(@Param("userId") UUID userId, @Param("from") LocalDate from);

    @Query("select b from Booking b where b.trainer.id = :trainerId and b.status = com.findmycoach.booking.BookingStatus.CONFIRMED and b.date = :date")
    List<Booking> findConfirmedForTrainerOn(@Param("trainerId") String trainerId, @Param("date") LocalDate date);
}

package com.findmycoach.venue;

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

public interface VenueBookingRepository extends JpaRepository<VenueBooking, UUID> {

    List<VenueBooking> findByVenueIdAndDateBetweenAndStatusIn(String venueId, LocalDate from, LocalDate to,
                                                              Collection<VenueBookingStatus> statuses);

    @EntityGraph(attributePaths = "venue")
    List<VenueBooking> findByUserIdOrderByDateAscStartTimeAsc(UUID userId);

    @EntityGraph(attributePaths = {"venue", "user"})
    @Query("""
            select b from VenueBooking b
            where (:venueId is null or b.venue.id = :venueId)
              and (:status is null or b.status = :status)
              and (:from is null or b.date >= :from)
              and (:to is null or b.date <= :to)
            """)
    Page<VenueBooking> search(@Param("venueId") String venueId, @Param("status") VenueBookingStatus status,
                              @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    long countByStatus(VenueBookingStatus status);

    @Query("select coalesce(sum(b.totalPrice), 0) from VenueBooking b where b.status <> com.findmycoach.venue.VenueBookingStatus.CANCELLED")
    BigDecimal totalRevenue();

    @Query("select b from VenueBooking b where b.user.id = :userId and b.status = com.findmycoach.venue.VenueBookingStatus.CONFIRMED and b.date >= :from")
    List<VenueBooking> findActiveFutureForUser(@Param("userId") UUID userId, @Param("from") LocalDate from);
}

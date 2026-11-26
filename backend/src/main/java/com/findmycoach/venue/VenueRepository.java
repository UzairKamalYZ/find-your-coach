package com.findmycoach.venue;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface VenueRepository extends JpaRepository<Venue, String> {

    long countByActiveTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Venue v where v.id = :id")
    Optional<Venue> findByIdForUpdate(@Param("id") String id);

    @Query("""
            select v from Venue v
            where (:includeInactive = true or v.active = true)
              and (:sport is null or lower(v.sport) = lower(:sport))
              and (:city is null or lower(v.city) like lower(concat('%', :city, '%')))
              and (:type is null or v.type = :type)
              and (:q is null or lower(v.name) like lower(concat('%', :q, '%'))
                   or lower(v.city) like lower(concat('%', :q, '%'))
                   or lower(coalesce(v.address, '')) like lower(concat('%', :q, '%')))
              and (:maxPrice is null or v.pricePerHour <= :maxPrice)
            """)
    Page<Venue> search(@Param("sport") String sport, @Param("city") String city, @Param("type") VenueType type,
                       @Param("q") String q, @Param("maxPrice") BigDecimal maxPrice,
                       @Param("includeInactive") boolean includeInactive, Pageable pageable);
}

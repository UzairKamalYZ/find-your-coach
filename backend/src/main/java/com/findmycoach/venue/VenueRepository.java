package com.findmycoach.venue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VenueRepository extends JpaRepository<Venue, String> {

    @Query("""
        SELECT v FROM Venue v
        WHERE (:sport IS NULL OR v.sport = :sport)
        AND (:city IS NULL OR LOWER(v.city) = LOWER(:city))
        """)
    Page<Venue> search(@Param("sport") String sport,
                       @Param("city") String city,
                       Pageable pageable);
}

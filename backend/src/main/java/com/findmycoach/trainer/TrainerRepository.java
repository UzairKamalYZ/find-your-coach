package com.findmycoach.trainer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainerRepository extends JpaRepository<Trainer, String> {

    @Query("""
        SELECT t FROM Trainer t
        WHERE (:sport IS NULL OR t.sport = :sport)
        AND (:city IS NULL OR LOWER(t.city) = LOWER(:city))
        AND (:q IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(t.city) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Trainer> search(@Param("sport") String sport,
                         @Param("city") String city,
                         @Param("q") String q,
                         Pageable pageable);
}

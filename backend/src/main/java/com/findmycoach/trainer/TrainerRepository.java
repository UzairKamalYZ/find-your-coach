package com.findmycoach.trainer;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainerRepository extends JpaRepository<Trainer, String> {

    Optional<Trainer> findByUserId(UUID userId);

    long countByStatus(TrainerStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trainer t where t.id = :id")
    Optional<Trainer> findByIdForUpdate(@Param("id") String id);

    @Query("""
            select t from Trainer t
            where t.status in :statuses
              and (:sport is null or lower(t.sport) = lower(:sport))
              and (:city is null or lower(t.city) like lower(concat('%', :city, '%')))
              and (:q is null or lower(t.name) like lower(concat('%', :q, '%'))
                   or lower(t.city) like lower(concat('%', :q, '%'))
                   or lower(t.sport) like lower(concat('%', :q, '%'))
                   or lower(t.specialties) like lower(concat('%', :q, '%')))
              and (:minRate is null or t.hourlyRate >= :minRate)
              and (:maxRate is null or t.hourlyRate <= :maxRate)
              and (:minRating is null or t.rating >= :minRating)
              and (:featured is null or t.featured = :featured)
            """)
    Page<Trainer> search(@Param("statuses") Collection<TrainerStatus> statuses,
                         @Param("sport") String sport,
                         @Param("city") String city,
                         @Param("q") String q,
                         @Param("minRate") BigDecimal minRate,
                         @Param("maxRate") BigDecimal maxRate,
                         @Param("minRating") Double minRating,
                         @Param("featured") Boolean featured,
                         Pageable pageable);

    @Query("select distinct t.city from Trainer t where t.status = com.findmycoach.trainer.TrainerStatus.ACTIVE order by t.city")
    List<String> findActiveCities();
}

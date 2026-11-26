package com.findmycoach.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @EntityGraph(attributePaths = "author")
    Page<Review> findByTrainerIdAndHiddenFalse(String trainerId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "trainer"})
    Page<Review> findByTrainerId(String trainerId, Pageable pageable);

    boolean existsByBookingId(UUID bookingId);

    @Query("select r.booking.id from Review r where r.booking.id in :ids")
    List<UUID> findReviewedBookingIds(@Param("ids") Collection<UUID> ids);

    @EntityGraph(attributePaths = {"author", "trainer"})
    @Query("""
            select r from Review r
            where (:trainerId is null or r.trainer.id = :trainerId)
              and (:hidden is null or r.hidden = :hidden)
              and (:maxRating is null or r.rating <= :maxRating)
            """)
    Page<Review> search(@Param("trainerId") String trainerId, @Param("hidden") Boolean hidden,
                        @Param("maxRating") Integer maxRating, Pageable pageable);

    @Query("select r.rating, count(r) from Review r where r.trainer.id = :trainerId and r.hidden = false group by r.rating")
    List<Object[]> ratingBreakdown(@Param("trainerId") String trainerId);
}

package com.findmycoach.review;

import com.findmycoach.audit.AuditService;
import com.findmycoach.booking.Booking;
import com.findmycoach.booking.BookingRepository;
import com.findmycoach.booking.BookingStatus;
import com.findmycoach.common.Slots;
import com.findmycoach.common.Strings;
import com.findmycoach.common.exception.ApiException;
import com.findmycoach.common.web.PageResponse;
import com.findmycoach.common.web.Pagination;
import com.findmycoach.notification.NotificationService;
import com.findmycoach.notification.NotificationType;
import com.findmycoach.review.dto.*;
import com.findmycoach.security.AuthUser;
import com.findmycoach.trainer.Trainer;
import com.findmycoach.trainer.TrainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final TrainerRepository trainerRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> listForTrainer(String trainerId, int page, int size) {
        var result = reviewRepository.findByTrainerIdAndHiddenFalse(trainerId,
                Pagination.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(result, ReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> listAllForTrainer(String trainerId, int page, int size) {
        var result = reviewRepository.findByTrainerId(trainerId,
                Pagination.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(result, ReviewResponse::from);
    }

    @Transactional(readOnly = true)
    public ReviewSummary summary(String trainerId) {
        Trainer t = trainerRepository.findById(trainerId).orElseThrow(() -> ApiException.notFound("Coach not found"));
        Map<Integer, Long> breakdown = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) breakdown.put(i, 0L);
        for (Object[] row : reviewRepository.ratingBreakdown(trainerId)) {
            breakdown.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        return new ReviewSummary(t.getRating(), t.getReviewCount(), breakdown);
    }

    @Transactional
    public ReviewResponse create(AuthUser me, CreateReviewRequest req) {
        Booking b = bookingRepository.findById(req.bookingId()).orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (!b.getTrainee().getId().equals(me.id())) throw ApiException.notFound("Booking not found");
        if (b.getStatus() == BookingStatus.CANCELLED || b.getStatus() == BookingStatus.NO_SHOW) {
            throw ApiException.unprocessable("Only attended sessions can be reviewed");
        }
        LocalDateTime start = b.getDate().atTime(Slots.toTime(b.getSlot()));
        if (start.isAfter(LocalDateTime.now(clock))) {
            throw ApiException.unprocessable("You can review a session once it has taken place");
        }
        if (reviewRepository.existsByBookingId(b.getId())) throw ApiException.conflict("You already reviewed this session");

        Trainer trainer = trainerRepository.findByIdForUpdate(b.getTrainer().getId()).orElseThrow();
        Review r = new Review();
        r.setBooking(b);
        r.setTrainer(trainer);
        r.setAuthor(b.getTrainee());
        r.setRating(req.rating());
        r.setComment(Strings.clean(req.comment()));
        reviewRepository.save(r);
        trainer.addRating(req.rating());

        notificationService.notify(trainer.getUser(), NotificationType.REVIEW_RECEIVED, "New " + req.rating() + "-star review",
                b.getTrainee().getName() + " reviewed their session.", "/coach");
        return ReviewResponse.from(r);
    }

    @Transactional
    public ReviewResponse update(AuthUser me, UUID id, UpdateReviewRequest req) {
        Review r = require(id);
        if (!r.getAuthor().getId().equals(me.id())) throw ApiException.notFound("Review not found");
        if (req.rating() != null && req.rating() != r.getRating()) {
            Trainer t = trainerRepository.findByIdForUpdate(r.getTrainer().getId()).orElseThrow();
            if (!r.isHidden()) {
                t.removeRating(r.getRating());
                t.addRating(req.rating());
            }
            r.setRating(req.rating());
        }
        if (req.comment() != null) r.setComment(Strings.clean(req.comment()));
        return ReviewResponse.from(r);
    }

    @Transactional
    public void delete(AuthUser me, UUID id) {
        Review r = require(id);
        boolean author = r.getAuthor().getId().equals(me.id());
        if (!author && !me.isAdmin()) throw ApiException.notFound("Review not found");
        if (!r.isHidden()) {
            trainerRepository.findByIdForUpdate(r.getTrainer().getId()).orElseThrow().removeRating(r.getRating());
        }
        reviewRepository.delete(r);
        if (!author) auditService.record(me, "REVIEW_DELETED", "REVIEW", id, r.getComment());
    }

    @Transactional
    public ReviewResponse reply(AuthUser me, UUID id, ReplyRequest req) {
        Review r = require(id);
        var coach = r.getTrainer().getUser();
        if (coach == null || !coach.getId().equals(me.id())) throw ApiException.notFound("Review not found");
        r.setReply(req.reply().trim());
        r.setRepliedAt(Instant.now(clock));
        notificationService.notify(r.getAuthor(), NotificationType.REVIEW_REPLY, r.getTrainer().getName() + " replied to your review",
                r.getReply(), "/trainers/" + r.getTrainer().getId());
        return ReviewResponse.from(r);
    }

    /** Moderation: hiding removes the review from public pages and from the coach's rating. */
    @Transactional
    public ReviewResponse setHidden(AuthUser admin, UUID id, boolean hidden) {
        Review r = require(id);
        if (r.isHidden() != hidden) {
            Trainer t = trainerRepository.findByIdForUpdate(r.getTrainer().getId()).orElseThrow();
            if (hidden) t.removeRating(r.getRating());
            else t.addRating(r.getRating());
            r.setHidden(hidden);
            auditService.record(admin, hidden ? "REVIEW_HIDDEN" : "REVIEW_UNHIDDEN", "REVIEW", id, null);
        }
        return ReviewResponse.from(r);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> adminSearch(String trainerId, Boolean hidden, Integer maxRating, int page, int size) {
        var result = reviewRepository.search(Strings.clean(trainerId), hidden, maxRating,
                Pagination.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(result, ReviewResponse::from);
    }

    private Review require(UUID id) {
        return reviewRepository.findById(id).orElseThrow(() -> ApiException.notFound("Review not found"));
    }
}

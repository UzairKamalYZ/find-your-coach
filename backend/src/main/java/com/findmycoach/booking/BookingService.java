package com.findmycoach.booking;

import com.findmycoach.audit.AuditService;
import com.findmycoach.booking.dto.*;
import com.findmycoach.common.Slots;
import com.findmycoach.common.Strings;
import com.findmycoach.common.exception.ApiException;
import com.findmycoach.common.web.PageResponse;
import com.findmycoach.common.web.Pagination;
import com.findmycoach.config.AppProperties;
import com.findmycoach.notification.NotificationService;
import com.findmycoach.notification.NotificationType;
import com.findmycoach.review.ReviewRepository;
import com.findmycoach.security.AuthUser;
import com.findmycoach.trainer.Trainer;
import com.findmycoach.trainer.TrainerRepository;
import com.findmycoach.trainer.TrainerService;
import com.findmycoach.user.Role;
import com.findmycoach.user.User;
import com.findmycoach.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    static final int MAX_RESCHEDULES = 3;
    private static final Set<BookingStatus> SLOT_HOLDING = EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED, BookingStatus.NO_SHOW);
    private static final DateTimeFormatter HUMAN_DATE = DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK);

    private final BookingRepository bookingRepository;
    private final TrainerRepository trainerRepository;
    private final TrainerService trainerService;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final AppProperties props;
    private final Clock clock;

    // ---------------------------------------------------------------- trainee actions

    @Transactional
    public BookingResponse create(AuthUser me, CreateBookingRequest req) {
        User trainee = userRepository.findById(me.id()).orElseThrow(() -> ApiException.unauthorized("Account not found"));
        // Lock the coach row so two concurrent requests for the same slot are serialised.
        Trainer trainer = trainerRepository.findByIdForUpdate(req.trainerId())
                .orElseThrow(() -> ApiException.notFound("Coach not found"));
        if (!trainer.isBookable()) throw ApiException.unprocessable("This coach is not taking bookings right now");
        if (trainer.getUser() != null && trainer.getUser().getId().equals(me.id())) {
            throw ApiException.unprocessable("You cannot book a session with yourself");
        }
        validateSlotIsBookable(trainer, req.date(), req.slot());
        if (bookingRepository.existsByTraineeIdAndDateAndSlotAndStatus(me.id(), req.date(), req.slot(), BookingStatus.CONFIRMED)) {
            throw ApiException.conflict("You already have a session booked at this time");
        }

        Booking b = new Booking();
        b.setTrainee(trainee);
        b.setTrainer(trainer);
        b.setTrainerName(trainer.getName());
        b.setTrainerImage(trainer.getImage());
        b.setSport(trainer.getSport());
        b.setDate(req.date());
        b.setSlot(req.slot());
        b.setPrice(trainer.getHourlyRate());
        b.setNotes(Strings.clean(req.notes()));
        bookingRepository.save(b);

        String when = describe(b);
        notificationService.notify(trainee, NotificationType.BOOKING_CONFIRMED, "Booking confirmed",
                "Your " + b.getSport() + " session with " + trainer.getName() + " is booked for " + when + ".", "/my-calendar");
        notificationService.notify(trainer.getUser(), NotificationType.BOOKING_RECEIVED, "New booking",
                trainee.getName() + " booked a session on " + when + ".", "/coach");
        return toResponse(b, me);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listMine(AuthUser me, String scope) {
        List<Booking> all = bookingRepository.findByTraineeIdOrderByDateAscSlotAsc(me.id());
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        String s = scope == null ? "active" : scope.toLowerCase(Locale.ROOT);
        List<Booking> filtered = all.stream().filter(b -> switch (s) {
            case "upcoming" -> b.getStatus() == BookingStatus.CONFIRMED && !b.getDate().isBefore(today);
            case "past" -> b.getStatus() != BookingStatus.CANCELLED && b.getDate().isBefore(today)
                    || b.getStatus() == BookingStatus.COMPLETED || b.getStatus() == BookingStatus.NO_SHOW;
            case "cancelled" -> b.getStatus() == BookingStatus.CANCELLED;
            case "all" -> true;
            default -> b.getStatus() != BookingStatus.CANCELLED; // "active"
        }).toList();
        return toResponses(filtered, me);
    }

    @Transactional(readOnly = true)
    public BookingResponse get(AuthUser me, UUID id) {
        Booking b = require(id);
        if (!canView(me, b)) throw ApiException.notFound("Booking not found");
        return toResponse(b, me);
    }

    @Transactional
    public BookingResponse cancel(AuthUser me, UUID id, String reason) {
        Booking b = require(id);
        if (!canView(me, b)) throw ApiException.notFound("Booking not found");
        if (b.getStatus() != BookingStatus.CONFIRMED) {
            throw ApiException.unprocessable("Only confirmed bookings can be cancelled");
        }
        boolean isTrainee = b.getTrainee().getId().equals(me.id());
        boolean isCoach = isCoachOf(me, b);
        if (!me.isAdmin() && hasStarted(b)) {
            throw ApiException.unprocessable("This session has already started and can no longer be cancelled");
        }
        Role by = me.isAdmin() && !isTrainee && !isCoach ? Role.ADMIN : isCoach ? Role.TRAINER : Role.TRAINEE;
        b.setStatus(BookingStatus.CANCELLED);
        b.setCancelledBy(by);
        b.setCancelledAt(clock.instant());
        b.setCancellationReason(Strings.clean(reason));
        b.setLateCancellation(by == Role.TRAINEE && hoursUntilStart(b) < props.booking().freeCancellationHours());

        String when = describe(b);
        String why = b.getCancellationReason() == null ? "" : " Reason: " + b.getCancellationReason();
        if (by != Role.TRAINEE) {
            notificationService.notify(b.getTrainee(), NotificationType.BOOKING_CANCELLED, "Session cancelled",
                    "Your session with " + b.getTrainerName() + " on " + when + " was cancelled"
                            + (by == Role.ADMIN ? " by FindMyTrainer support." : " by the coach.") + why, "/my-calendar");
        }
        if (by != Role.TRAINER) {
            notificationService.notify(b.getTrainer().getUser(), NotificationType.BOOKING_CANCELLED, "Booking cancelled",
                    b.getTrainee().getName() + "'s session on " + when + " was cancelled." + why, "/coach");
        }
        if (by == Role.ADMIN) auditService.record(me, "BOOKING_CANCELLED", "BOOKING", b.getId(), b.getCancellationReason());
        return toResponse(b, me);
    }

    @Transactional
    public BookingResponse reschedule(AuthUser me, UUID id, RescheduleRequest req) {
        Booking b = require(id);
        if (!b.getTrainee().getId().equals(me.id())) throw ApiException.notFound("Booking not found");
        if (b.getStatus() != BookingStatus.CONFIRMED) throw ApiException.unprocessable("Only confirmed bookings can be rescheduled");
        if (hasStarted(b)) throw ApiException.unprocessable("This session has already started");
        if (b.getRescheduleCount() >= MAX_RESCHEDULES) {
            throw ApiException.unprocessable("A booking can be rescheduled at most " + MAX_RESCHEDULES + " times");
        }
        if (b.getDate().equals(req.date()) && b.getSlot().equals(req.slot())) {
            throw ApiException.badRequest("The new time is the same as the current one");
        }
        Trainer trainer = trainerRepository.findByIdForUpdate(b.getTrainer().getId()).orElseThrow();
        if (!trainer.isBookable()) throw ApiException.unprocessable("This coach is not taking bookings right now");
        validateSlotIsBookable(trainer, req.date(), req.slot());
        if (bookingRepository.existsByTraineeIdAndDateAndSlotAndStatus(me.id(), req.date(), req.slot(), BookingStatus.CONFIRMED)) {
            throw ApiException.conflict("You already have a session booked at this time");
        }
        String oldWhen = describe(b);
        b.setDate(req.date());
        b.setSlot(req.slot());
        b.setRescheduleCount(b.getRescheduleCount() + 1);
        notificationService.notify(trainer.getUser(), NotificationType.BOOKING_RESCHEDULED, "Booking moved",
                b.getTrainee().getName() + " moved their session from " + oldWhen + " to " + describe(b) + ".", "/coach");
        notificationService.notify(b.getTrainee(), NotificationType.BOOKING_RESCHEDULED, "Booking moved",
                "Your session with " + b.getTrainerName() + " is now on " + describe(b) + ".", "/my-calendar");
        return toResponse(b, me);
    }

    // ---------------------------------------------------------------- coach / admin actions

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> listForCoach(AuthUser me, String trainerId, BookingStatus status,
                                                      LocalDate from, LocalDate to, int page, int size) {
        Page<Booking> result = bookingRepository.searchForTrainer(trainerId, status, from, to,
                Pagination.of(page, size, Sort.by("date").ascending().and(Sort.by("slot").ascending())));
        return pageOf(result, me);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> adminSearch(AuthUser me, String trainerId, UUID traineeId, BookingStatus status,
                                                     LocalDate from, LocalDate to, int page, int size) {
        Page<Booking> result = bookingRepository.search(Strings.clean(trainerId), traineeId, status, from, to,
                Pagination.of(page, size, Sort.by("date").descending().and(Sort.by("slot").descending())));
        return pageOf(result, me);
    }

    /** Coach (for their own sessions) or admin records the outcome of a session. */
    @Transactional
    public BookingResponse updateStatus(AuthUser me, UUID id, BookingStatusUpdateRequest req) {
        Booking b = require(id);
        if (!me.isAdmin() && !isCoachOf(me, b)) throw ApiException.notFound("Booking not found");
        return switch (req.status()) {
            case CANCELLED -> cancel(me, id, req.reason());
            case COMPLETED, NO_SHOW -> {
                if (b.getStatus() == BookingStatus.CANCELLED) {
                    throw ApiException.unprocessable("A cancelled booking cannot be marked as " + req.status());
                }
                if (!hasStarted(b)) throw ApiException.unprocessable("A session can only be marked after it has started");
                b.setStatus(req.status());
                if (req.status() == BookingStatus.COMPLETED) {
                    notificationService.notify(b.getTrainee(), NotificationType.BOOKING_COMPLETED, "How was your session?",
                            "Leave a review for " + b.getTrainerName() + " to help other trainees.", "/my-calendar");
                }
                if (me.isAdmin()) auditService.record(me, "BOOKING_STATUS_" + req.status(), "BOOKING", b.getId(), req.reason());
                yield toResponse(b, me);
            }
            case CONFIRMED -> throw ApiException.badRequest("Use the booking endpoint to create a confirmed booking");
        };
    }

    /** Coach takes a day off and optionally cancels the sessions already booked on it. */
    @Transactional
    public int cancelForTimeOff(AuthUser coach, Trainer trainer, LocalDate date, String reason) {
        List<Booking> list = bookingRepository.findConfirmedForTrainerOn(trainer.getId(), date);
        for (Booking b : list) {
            b.setStatus(BookingStatus.CANCELLED);
            b.setCancelledBy(Role.TRAINER);
            b.setCancelledAt(clock.instant());
            b.setCancellationReason(reason == null ? "Coach unavailable" : reason);
            notificationService.notify(b.getTrainee(), NotificationType.BOOKING_CANCELLED, "Session cancelled",
                    b.getTrainerName() + " is unavailable on " + describe(b) + " and cancelled your session.", "/my-calendar");
        }
        return list.size();
    }

    @Transactional
    public void cancelAllFutureForAccountClosure(User user) {
        LocalDate today = LocalDate.now(clock);
        for (Booking b : bookingRepository.findActiveFutureForTrainee(user.getId(), today)) {
            b.setStatus(BookingStatus.CANCELLED);
            b.setCancelledBy(Role.TRAINEE);
            b.setCancelledAt(clock.instant());
            b.setCancellationReason("Trainee closed their account");
            notificationService.notify(b.getTrainer().getUser(), NotificationType.BOOKING_CANCELLED, "Booking cancelled",
                    "A session on " + describe(b) + " was cancelled because the trainee closed their account.", "/coach");
        }
        trainerRepository.findByUserId(user.getId()).ifPresent(t -> {
            LocalDate end = today.plusDays(props.booking().bookingWindowDays() + 1L);
            for (Booking b : bookingRepository.findByTrainerIdAndDateBetweenAndStatusIn(t.getId(), today, end, EnumSet.of(BookingStatus.CONFIRMED))) {
                b.setStatus(BookingStatus.CANCELLED);
                b.setCancelledBy(Role.TRAINER);
                b.setCancelledAt(clock.instant());
                b.setCancellationReason("Coach is no longer available");
                notificationService.notify(b.getTrainee(), NotificationType.BOOKING_CANCELLED, "Session cancelled",
                        b.getTrainerName() + " is no longer available; your session on " + describe(b) + " was cancelled.", "/my-calendar");
            }
        });
    }

    /** Nightly: confirmed sessions from previous days that nobody marked are treated as completed. */
    @Scheduled(cron = "0 15 2 * * *", zone = "${app.time-zone}")
    @Transactional
    public int autoCompletePastSessions() {
        List<Booking> stale = bookingRepository.findConfirmedBefore(LocalDate.now(clock));
        stale.forEach(b -> b.setStatus(BookingStatus.COMPLETED));
        if (!stale.isEmpty()) log.info("Auto-completed {} past sessions", stale.size());
        return stale.size();
    }

    // ---------------------------------------------------------------- helpers

    private void validateSlotIsBookable(Trainer trainer, LocalDate date, String slot) {
        if (!Slots.isValid(slot)) throw ApiException.badRequest("Unknown time slot: " + slot);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime start = date.atTime(Slots.toTime(slot));
        if (!start.isAfter(now)) throw ApiException.unprocessable("That time is in the past");
        LocalDate last = now.toLocalDate().plusDays(props.booking().bookingWindowDays() - 1L);
        if (date.isAfter(last)) {
            throw ApiException.unprocessable("Bookings can be made at most " + props.booking().bookingWindowDays() + " days ahead");
        }
        if (!trainerService.worksAt(trainer, date, slot)) {
            throw ApiException.unprocessable("The coach is not available at that time");
        }
        if (bookingRepository.existsByTrainerIdAndDateAndSlotAndStatusIn(trainer.getId(), date, slot, SLOT_HOLDING)) {
            throw ApiException.conflict("Slot already booked");
        }
    }

    Booking require(UUID id) {
        return bookingRepository.findById(id).orElseThrow(() -> ApiException.notFound("Booking not found"));
    }

    private boolean isCoachOf(AuthUser me, Booking b) {
        User coach = b.getTrainer().getUser();
        return coach != null && coach.getId().equals(me.id());
    }

    private boolean canView(AuthUser me, Booking b) {
        return me.isAdmin() || b.getTrainee().getId().equals(me.id()) || isCoachOf(me, b);
    }

    private LocalDateTime start(Booking b) {
        return b.getDate().atTime(Slots.toTime(b.getSlot()));
    }

    boolean hasStarted(Booking b) {
        return !start(b).isAfter(LocalDateTime.now(clock));
    }

    private long hoursUntilStart(Booking b) {
        return Duration.between(LocalDateTime.now(clock), start(b)).toHours();
    }

    private String describe(Booking b) {
        return b.getDate().format(HUMAN_DATE) + " at " + b.getSlot();
    }

    private BookingResponse toResponse(Booking b, AuthUser viewer) {
        boolean reviewed = reviewRepository.existsByBookingId(b.getId());
        return BookingResponse.from(b, flags(b, viewer, reviewed), !b.getTrainee().getId().equals(viewer.id()) || viewer.isAdmin());
    }

    private List<BookingResponse> toResponses(List<Booking> list, AuthUser viewer) {
        if (list.isEmpty()) return List.of();
        Set<UUID> reviewed = new HashSet<>(reviewRepository.findReviewedBookingIds(list.stream().map(Booking::getId).toList()));
        return list.stream().map(b -> BookingResponse.from(b, flags(b, viewer, reviewed.contains(b.getId())),
                !b.getTrainee().getId().equals(viewer.id()) || viewer.isAdmin())).toList();
    }

    private PageResponse<BookingResponse> pageOf(Page<Booking> page, AuthUser viewer) {
        List<BookingResponse> content = toResponses(page.getContent(), viewer);
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private BookingResponse.Flags flags(Booking b, AuthUser viewer, boolean reviewed) {
        boolean isTrainee = b.getTrainee().getId().equals(viewer.id());
        boolean started = hasStarted(b);
        boolean confirmed = b.getStatus() == BookingStatus.CONFIRMED;
        boolean canCancel = confirmed && (viewer.isAdmin() || (!started && (isTrainee || isCoachOf(viewer, b))));
        boolean canReschedule = isTrainee && confirmed && !started && b.getRescheduleCount() < MAX_RESCHEDULES;
        boolean canReview = isTrainee && !reviewed && started
                && (b.getStatus() == BookingStatus.COMPLETED || b.getStatus() == BookingStatus.CONFIRMED);
        return new BookingResponse.Flags(canCancel, canReschedule, canReview, reviewed);
    }
}

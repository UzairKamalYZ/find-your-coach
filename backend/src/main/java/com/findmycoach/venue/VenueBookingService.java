package com.findmycoach.venue;

import com.findmycoach.audit.AuditService;
import com.findmycoach.common.Strings;
import com.findmycoach.common.exception.ApiException;
import com.findmycoach.common.web.PageResponse;
import com.findmycoach.common.web.Pagination;
import com.findmycoach.config.AppProperties;
import com.findmycoach.notification.NotificationService;
import com.findmycoach.notification.NotificationType;
import com.findmycoach.security.AuthUser;
import com.findmycoach.user.User;
import com.findmycoach.user.UserRepository;
import com.findmycoach.venue.dto.CreateVenueBookingRequest;
import com.findmycoach.venue.dto.VenueBookingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VenueBookingService {
    private static final DateTimeFormatter HUMAN_DATE = DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK);

    private final VenueRepository venueRepository;
    private final VenueBookingRepository repo;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final AppProperties props;
    private final Clock clock;

    @Transactional
    public VenueBookingResponse create(AuthUser me, CreateVenueBookingRequest req) {
        User user = userRepository.findById(me.id()).orElseThrow(() -> ApiException.unauthorized("Account not found"));
        Venue v = venueRepository.findByIdForUpdate(req.venueId()).orElseThrow(() -> ApiException.notFound("Venue not found"));
        if (!v.isActive()) throw ApiException.unprocessable("This venue is not taking bookings right now");
        LocalTime start = LocalTime.parse(req.startTime());
        int hours = req.hours();
        LocalDateTime now = LocalDateTime.now(clock);
        if (!req.date().atTime(start).isAfter(now)) throw ApiException.unprocessable("That time is in the past");
        if (req.date().isAfter(now.toLocalDate().plusDays(props.booking().bookingWindowDays() - 1L))) {
            throw ApiException.unprocessable("Bookings can be made at most " + props.booking().bookingWindowDays() + " days ahead");
        }
        if (start.isBefore(v.getOpenTime()) || start.plusHours(hours).isAfter(v.getCloseTime())
                || start.plusHours(hours).isBefore(start)) {
            throw ApiException.unprocessable("The venue is open from " + v.getOpenTime() + " to " + v.getCloseTime());
        }
        List<VenueBooking> sameDay = repo.findByVenueIdAndDateBetweenAndStatusIn(v.getId(), req.date(), req.date(),
                EnumSet.of(VenueBookingStatus.CONFIRMED));
        for (int h = 0; h < hours; h++) {
            LocalTime hour = start.plusHours(h);
            long used = sameDay.stream().filter(b -> b.overlaps(hour, 1)).count();
            if (used >= v.getCapacity()) throw ApiException.conflict("No " + v.getType().label().toLowerCase(Locale.ROOT) + " free at " + hour);
        }
        VenueBooking b = new VenueBooking();
        b.setVenue(v);
        b.setUser(user);
        b.setDate(req.date());
        b.setStartTime(start);
        b.setHours(hours);
        b.setTotalPrice(v.getPricePerHour().multiply(BigDecimal.valueOf(hours)));
        b.setNotes(Strings.clean(req.notes()));
        repo.save(b);
        notificationService.notify(user, NotificationType.VENUE_BOOKING_CONFIRMED, "Venue reserved",
                v.getName() + " on " + b.getDate().format(HUMAN_DATE) + ", " + start + "–" + b.endTime() + ".", "/my-calendar");
        return toResponse(b, me);
    }

    @Transactional(readOnly = true)
    public List<VenueBookingResponse> listMine(AuthUser me, boolean includeCancelled) {
        return repo.findByUserIdOrderByDateAscStartTimeAsc(me.id()).stream()
                .filter(b -> includeCancelled || b.getStatus() != VenueBookingStatus.CANCELLED)
                .map(b -> toResponse(b, me)).toList();
    }

    @Transactional
    public VenueBookingResponse cancel(AuthUser me, UUID id, String reason) {
        VenueBooking b = repo.findById(id).orElseThrow(() -> ApiException.notFound("Booking not found"));
        boolean owner = b.getUser().getId().equals(me.id());
        if (!owner && !me.isAdmin()) throw ApiException.notFound("Booking not found");
        if (b.getStatus() != VenueBookingStatus.CONFIRMED) throw ApiException.unprocessable("Only confirmed bookings can be cancelled");
        if (!me.isAdmin() && !b.getDate().atTime(b.getStartTime()).isAfter(LocalDateTime.now(clock))) {
            throw ApiException.unprocessable("This booking has already started");
        }
        b.setStatus(VenueBookingStatus.CANCELLED);
        b.setCancelledAt(clock.instant());
        b.setCancellationReason(Strings.clean(reason));
        if (!owner) {
            notificationService.notify(b.getUser(), NotificationType.VENUE_BOOKING_CANCELLED, "Venue booking cancelled",
                    "Your booking at " + b.getVenue().getName() + " on " + b.getDate().format(HUMAN_DATE)
                            + " was cancelled by FindMyTrainer support.", "/my-calendar");
            auditService.record(me, "VENUE_BOOKING_CANCELLED", "VENUE_BOOKING", id, reason);
        }
        return toResponse(b, me);
    }

    @Transactional
    public void cancelAllFutureForUser(User user, String reason) {
        for (VenueBooking b : repo.findActiveFutureForUser(user.getId(), LocalDate.now(clock))) {
            b.setStatus(VenueBookingStatus.CANCELLED);
            b.setCancelledAt(clock.instant());
            b.setCancellationReason(reason);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<VenueBookingResponse> adminSearch(AuthUser admin, String venueId, VenueBookingStatus status,
                                                          LocalDate from, LocalDate to, int page, int size) {
        var result = repo.search(Strings.clean(venueId), status, from, to,
                Pagination.of(page, size, Sort.by("date").descending().and(Sort.by("startTime").descending())));
        return PageResponse.of(result, b -> toResponse(b, admin));
    }

    private VenueBookingResponse toResponse(VenueBooking b, AuthUser viewer) {
        boolean started = !b.getDate().atTime(b.getStartTime()).isAfter(LocalDateTime.now(clock));
        boolean canCancel = b.getStatus() == VenueBookingStatus.CONFIRMED && (viewer.isAdmin() || !started);
        return VenueBookingResponse.from(b, canCancel, true);
    }
}

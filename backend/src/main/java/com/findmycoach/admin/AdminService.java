package com.findmycoach.admin;

import com.findmycoach.admin.dto.*;
import com.findmycoach.audit.AuditLog;
import com.findmycoach.audit.AuditLogRepository;
import com.findmycoach.audit.AuditService;
import com.findmycoach.auth.RefreshTokenService;
import com.findmycoach.booking.BookingRepository;
import com.findmycoach.booking.BookingStatus;
import com.findmycoach.coach.CoachService;
import com.findmycoach.common.Slugs;
import com.findmycoach.common.Strings;
import com.findmycoach.common.exception.ApiException;
import com.findmycoach.common.web.PageResponse;
import com.findmycoach.common.web.Pagination;
import com.findmycoach.notification.NotificationService;
import com.findmycoach.notification.NotificationType;
import com.findmycoach.security.AuthUser;
import com.findmycoach.sport.SportService;
import com.findmycoach.trainer.*;
import com.findmycoach.trainer.dto.TrainerProfileRequest;
import com.findmycoach.trainer.dto.TrainerResponse;
import com.findmycoach.user.Role;
import com.findmycoach.user.User;
import com.findmycoach.user.UserRepository;
import com.findmycoach.user.UserService;
import com.findmycoach.user.dto.UserResponse;
import com.findmycoach.venue.VenueBookingRepository;
import com.findmycoach.venue.VenueBookingStatus;
import com.findmycoach.venue.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final UserService userService;
    private final TrainerRepository trainerRepository;
    private final BookingRepository bookingRepository;
    private final VenueRepository venueRepository;
    private final VenueBookingRepository venueBookingRepository;
    private final SportService sportService;
    private final NotificationService notificationService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardStats dashboard() {
        Map<String, Long> usersByRole = new LinkedHashMap<>();
        for (Role r : Role.values()) usersByRole.put(r.json(), userRepository.countByRole(r));
        Map<String, Long> trainersByStatus = new LinkedHashMap<>();
        for (TrainerStatus s : TrainerStatus.values()) trainersByStatus.put(s.name(), trainerRepository.countByStatus(s));
        Map<String, Long> bookingsByStatus = new LinkedHashMap<>();
        for (BookingStatus s : BookingStatus.values()) bookingsByStatus.put(s.name(), bookingRepository.countByStatus(s));

        List<DashboardStats.TopTrainer> top = bookingRepository.topTrainers(PageRequest.of(0, 5)).stream()
                .map(r -> new DashboardStats.TopTrainer((String) r[0], (String) r[1], ((Number) r[2]).longValue(), (BigDecimal) r[3]))
                .toList();
        Map<String, Long> bySport = new LinkedHashMap<>();
        bookingRepository.countBySport().forEach(r -> bySport.put((String) r[0], ((Number) r[1]).longValue()));

        LocalDate today = LocalDate.now(clock);
        Map<LocalDate, Long> perDay = new HashMap<>();
        bookingRepository.countPerDay(today, today.plusDays(13)).forEach(r -> perDay.put((LocalDate) r[0], ((Number) r[1]).longValue()));
        List<DashboardStats.DayCount> next14 = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            LocalDate d = today.plusDays(i);
            next14.add(new DashboardStats.DayCount(d, perDay.getOrDefault(d, 0L)));
        }
        long total = userRepository.count();
        long disabled = userRepository.search(null, null, false, PageRequest.of(0, 1)).getTotalElements();
        return new DashboardStats(usersByRole, total, disabled, trainersByStatus, venueRepository.countByActiveTrue(),
                bookingsByStatus, bookingRepository.countCreatedSince(Instant.now(clock).minus(7, ChronoUnit.DAYS)),
                venueBookingRepository.count() - venueBookingRepository.countByStatus(VenueBookingStatus.CANCELLED),
                bookingRepository.completedRevenue(), bookingRepository.pipelineRevenue(), venueBookingRepository.totalRevenue(),
                top, bySport, next14);
    }

    // ------------------------------------------------------------ users

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> users(String q, Role role, Boolean enabled, int page, int size) {
        var result = userRepository.search(Strings.clean(q), role, enabled,
                Pagination.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(result, userService::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse user(UUID id) {
        return userService.toResponse(userService.require(id));
    }

    @Transactional
    public UserResponse updateUser(AuthUser admin, UUID id, AdminUserUpdateRequest req) {
        User u = userService.require(id);
        if (u.getId().equals(admin.id()) && (req.enabled() == Boolean.FALSE || (req.role() != null && req.role() != Role.ADMIN))) {
            throw ApiException.unprocessable("You cannot disable or demote your own admin account");
        }
        if (u.isAdmin() && (req.enabled() == Boolean.FALSE || (req.role() != null && req.role() != Role.ADMIN))
                && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw ApiException.unprocessable("At least one active admin is required");
        }
        List<String> changes = new ArrayList<>();
        if (req.role() != null && req.role() != u.getRole()) {
            if (u.getRole() == Role.TRAINER && trainerRepository.findByUserId(u.getId()).isPresent() && req.role() == Role.TRAINEE) {
                throw ApiException.unprocessable("Suspend or reassign this user's coaching listing before changing their role");
            }
            changes.add("role " + u.getRole() + " -> " + req.role());
            u.setRole(req.role());
            notificationService.notify(u, NotificationType.ACCOUNT, "Your account type changed",
                    "Your account is now a " + req.role().json() + " account.", "/profile");
        }
        if (req.enabled() != null && req.enabled() != u.isEnabled()) {
            changes.add(req.enabled() ? "enabled" : "disabled");
            u.setEnabled(req.enabled());
            if (!req.enabled()) refreshTokenService.revokeAll(u);
        }
        if (!changes.isEmpty()) auditService.record(admin, "USER_UPDATED", "USER", id, String.join(", ", changes));
        return userService.toResponse(u);
    }

    // ------------------------------------------------------------ trainers

    @Transactional(readOnly = true)
    public PageResponse<TrainerResponse> trainers(String q, TrainerStatus status, String sport, int page, int size) {
        Collection<TrainerStatus> statuses = status == null ? EnumSet.allOf(TrainerStatus.class) : EnumSet.of(status);
        var result = trainerRepository.search(statuses, Strings.clean(sport), null, Strings.clean(q), null, null, null, null,
                Pagination.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(result, TrainerResponse::from);
    }

    @Transactional
    public TrainerResponse setTrainerStatus(AuthUser admin, String id, TrainerStatusRequest req) {
        Trainer t = trainerRepository.findById(id).orElseThrow(() -> ApiException.notFound("Coach not found"));
        TrainerStatus old = t.getStatus();
        t.setStatus(req.status());
        t.setStatusReason(Strings.clean(req.reason()));
        if (old != req.status()) {
            String msg = switch (req.status()) {
                case ACTIVE -> "Your coaching listing is live — trainees can now book you.";
                case SUSPENDED -> "Your coaching listing has been suspended." + reasonSuffix(req.reason());
                case REJECTED -> "Your coaching listing needs changes before it can go live." + reasonSuffix(req.reason());
                case PENDING -> "Your coaching listing is awaiting review.";
            };
            notificationService.notify(t.getUser(), NotificationType.LISTING_STATUS, "Listing " + req.status().name().toLowerCase(Locale.ROOT), msg, "/coach");
            auditService.record(admin, "TRAINER_STATUS", "TRAINER", id, old + " -> " + req.status() + reasonSuffix(req.reason()));
        }
        return TrainerResponse.from(t);
    }

    private static String reasonSuffix(String reason) {
        return Strings.isBlank(reason) ? "" : " Reason: " + reason.trim();
    }

    @Transactional
    public TrainerResponse setFeatured(AuthUser admin, String id, boolean featured) {
        Trainer t = trainerRepository.findById(id).orElseThrow(() -> ApiException.notFound("Coach not found"));
        t.setFeatured(featured);
        auditService.record(admin, featured ? "TRAINER_FEATURED" : "TRAINER_UNFEATURED", "TRAINER", id, null);
        return TrainerResponse.from(t);
    }

    @Transactional
    public TrainerResponse updateTrainer(AuthUser admin, String id, TrainerProfileRequest req) {
        Trainer t = trainerRepository.findById(id).orElseThrow(() -> ApiException.notFound("Coach not found"));
        CoachService.applyProfile(t, req, sportService);
        auditService.record(admin, "TRAINER_UPDATED", "TRAINER", id, null);
        return TrainerResponse.from(t);
    }

    @Transactional
    public TrainerResponse createTrainer(AuthUser admin, AdminTrainerCreateRequest req) {
        Trainer t = new Trainer();
        String base = Slugs.slugify(req.profile().name());
        String id = base;
        for (int i = 2; trainerRepository.existsById(id); i++) id = base + "-" + i;
        t.setId(id);
        if (!Strings.isBlank(req.ownerEmail())) {
            User owner = userRepository.findByEmailIgnoreCase(req.ownerEmail().trim())
                    .orElseThrow(() -> ApiException.notFound("No account with email " + req.ownerEmail()));
            if (owner.getRole() != Role.TRAINER) throw ApiException.unprocessable("The owner account must have the trainer role");
            if (trainerRepository.findByUserId(owner.getId()).isPresent()) throw ApiException.conflict("That account already has a listing");
            t.setUser(owner);
        }
        CoachService.applyProfile(t, req.profile(), sportService);
        t.setStatus(TrainerStatus.ACTIVE);
        t.setFeatured(Boolean.TRUE.equals(req.featured()));
        t.setWeeklySlots(TrainerService.defaultSchedule());
        trainerRepository.save(t);
        auditService.record(admin, "TRAINER_CREATED", "TRAINER", id, t.getName());
        return TrainerResponse.from(t);
    }

    // ------------------------------------------------------------ audit

    @Transactional(readOnly = true)
    public PageResponse<AuditLog> auditLogs(String action, String targetType, int page, int size) {
        return PageResponse.of(auditLogRepository.search(Strings.clean(action), Strings.clean(targetType),
                Pagination.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }
}

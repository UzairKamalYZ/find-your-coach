package com.findmycoach.coach;

import com.findmycoach.booking.BookingRepository;
import com.findmycoach.booking.BookingService;
import com.findmycoach.booking.BookingStatus;
import com.findmycoach.booking.dto.BookingResponse;
import com.findmycoach.common.Slots;
import com.findmycoach.common.Slugs;
import com.findmycoach.common.Strings;
import com.findmycoach.common.exception.ApiException;
import com.findmycoach.favorite.FavoriteRepository;
import com.findmycoach.security.AuthUser;
import com.findmycoach.sport.SportService;
import com.findmycoach.trainer.*;
import com.findmycoach.trainer.dto.*;
import com.findmycoach.user.User;
import com.findmycoach.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CoachService {
    private static final int MAX_TIME_OFF_DAYS = 60;

    private final TrainerRepository trainerRepository;
    private final TimeOffRepository timeOffRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final SportService sportService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Trainer requireOwnListing(AuthUser me) {
        return trainerRepository.findByUserId(me.id())
                .orElseThrow(() -> ApiException.notFound("You have not created a coaching listing yet"));
    }

    @Transactional(readOnly = true)
    public TrainerResponse getListing(AuthUser me) {
        return TrainerResponse.from(requireOwnListing(me));
    }

    /** Creates the listing on first save (status PENDING until an admin approves it), updates it afterwards. */
    @Transactional
    public TrainerResponse saveListing(AuthUser me, TrainerProfileRequest req) {
        User user = userRepository.findById(me.id()).orElseThrow();
        Trainer t = trainerRepository.findByUserId(me.id()).orElse(null);
        boolean created = t == null;
        if (created) {
            t = new Trainer();
            t.setId(uniqueId(Slugs.slugify(req.name())));
            t.setUser(user);
            t.setStatus(TrainerStatus.PENDING);
            t.setWeeklySlots(TrainerService.defaultSchedule());
        } else if (t.getStatus() == TrainerStatus.REJECTED) {
            t.setStatus(TrainerStatus.PENDING); // resubmitted for review
            t.setStatusReason(null);
        }
        applyProfile(t, req, sportService);
        trainerRepository.save(t);
        if (Strings.isBlank(user.getSports())) user.setSports(t.getSport());
        if (Strings.isBlank(user.getCity())) user.setCity(t.getCity());
        return TrainerResponse.from(t);
    }

    public static void applyProfile(Trainer t, TrainerProfileRequest req, SportService sportService) {
        t.setName(req.name().trim());
        t.setSport(sportService.requireActive(req.sport()));
        t.setCity(req.city().trim());
        t.setHourlyRate(req.hourlyRate());
        t.setExperience(req.experience());
        t.setBio(Strings.clean(req.bio()));
        if (req.image() != null) t.setImage(Strings.clean(req.image()));
        t.setSpecialties(req.specialties() == null ? new ArrayList<>()
                : new ArrayList<>(req.specialties().stream().map(String::trim).distinct().toList()));
        t.setQualifications(Strings.clean(req.qualifications()));
    }

    @Transactional(readOnly = true)
    public WeeklyScheduleResponse getSchedule(AuthUser me) {
        return scheduleOf(requireOwnListing(me));
    }

    @Transactional
    public WeeklyScheduleResponse saveSchedule(AuthUser me, WeeklyScheduleRequest req) {
        Trainer t = requireOwnListing(me);
        Set<WeeklySlot> slots = new HashSet<>();
        req.days().forEach((day, list) -> {
            if (list == null) return;
            for (String s : list) {
                if (!Slots.isValid(s)) throw ApiException.badRequest("Unknown slot '" + s + "'. Valid slots: " + Slots.ALL);
                slots.add(new WeeklySlot(day, s));
            }
        });
        t.getWeeklySlots().clear();
        t.getWeeklySlots().addAll(slots);
        return scheduleOf(t);
    }

    static WeeklyScheduleResponse scheduleOf(Trainer t) {
        Map<DayOfWeek, List<String>> days = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) days.put(d, new ArrayList<>());
        t.getWeeklySlots().forEach(ws -> days.get(ws.getDayOfWeek()).add(ws.getSlot()));
        days.values().forEach(l -> l.sort(Comparator.naturalOrder()));
        return new WeeklyScheduleResponse(days, Slots.ALL);
    }

    @Transactional(readOnly = true)
    public List<TimeOffResponse> listTimeOff(AuthUser me) {
        Trainer t = requireOwnListing(me);
        return timeOffRepository.findByTrainerIdAndDateGreaterThanEqualOrderByDateAsc(t.getId(), LocalDate.now(clock))
                .stream().map(TimeOffResponse::from).toList();
    }

    public record TimeOffResult(List<TimeOffResponse> added, int cancelledBookings) {
    }

    @Transactional
    public TimeOffResult addTimeOff(AuthUser me, TimeOffRequest req) {
        Trainer t = requireOwnListing(me);
        LocalDate to = req.to() == null ? req.from() : req.to();
        if (to.isBefore(req.from())) throw ApiException.badRequest("'to' must not be before 'from'");
        if (req.from().isBefore(LocalDate.now(clock))) throw ApiException.badRequest("Time off cannot start in the past");
        if (Duration.between(req.from().atStartOfDay(), to.atStartOfDay()).toDays() + 1 > MAX_TIME_OFF_DAYS) {
            throw ApiException.badRequest("Time off can cover at most " + MAX_TIME_OFF_DAYS + " days at once");
        }
        boolean cancel = Boolean.TRUE.equals(req.cancelExistingBookings());
        List<TimeOffResponse> added = new ArrayList<>();
        int cancelled = 0;
        for (LocalDate d = req.from(); !d.isAfter(to); d = d.plusDays(1)) {
            List<?> existing = bookingRepository.findConfirmedForTrainerOn(t.getId(), d);
            if (!existing.isEmpty() && !cancel) {
                throw ApiException.conflict("You have " + existing.size() + " session(s) booked on " + d
                        + ". Set cancelExistingBookings=true to cancel them.");
            }
        }
        for (LocalDate d = req.from(); !d.isAfter(to); d = d.plusDays(1)) {
            if (timeOffRepository.existsByTrainerIdAndDate(t.getId(), d)) continue;
            if (cancel) cancelled += bookingService.cancelForTimeOff(me, t, d, Strings.clean(req.reason()));
            TimeOff o = new TimeOff();
            o.setTrainer(t);
            o.setDate(d);
            o.setReason(Strings.clean(req.reason()));
            added.add(TimeOffResponse.from(timeOffRepository.save(o)));
        }
        return new TimeOffResult(added, cancelled);
    }

    @Transactional
    public void removeTimeOff(AuthUser me, UUID id) {
        Trainer t = requireOwnListing(me);
        TimeOff o = timeOffRepository.findById(id).orElseThrow(() -> ApiException.notFound("Time off not found"));
        if (!o.getTrainer().getId().equals(t.getId())) throw ApiException.notFound("Time off not found");
        timeOffRepository.delete(o);
    }

    @Transactional(readOnly = true)
    public CoachStats stats(AuthUser me) {
        Trainer t = requireOwnListing(me);
        LocalDate today = LocalDate.now(clock);
        LocalDate monthStart = today.withDayOfMonth(1);
        List<BookingResponse> next = bookingService.listForCoach(me, t.getId(), BookingStatus.CONFIRMED, today, null, 0, 5).content();
        return new CoachStats(
                t.getStatus(),
                bookingRepository.searchForTrainer(t.getId(), BookingStatus.CONFIRMED, today, null,
                        org.springframework.data.domain.Pageable.ofSize(1)).getTotalElements(),
                bookingRepository.countByTrainerIdAndStatus(t.getId(), BookingStatus.COMPLETED),
                bookingRepository.countByTrainerIdAndStatus(t.getId(), BookingStatus.NO_SHOW),
                bookingRepository.countByTrainerIdAndStatus(t.getId(), BookingStatus.CANCELLED),
                bookingRepository.countDistinctTrainees(t.getId()),
                bookingRepository.sumPriceForTrainer(t.getId(), BookingStatus.COMPLETED),
                bookingRepository.completedEarningsBetween(t.getId(), monthStart, today),
                bookingRepository.sumPriceForTrainer(t.getId(), BookingStatus.CONFIRMED),
                t.getRating(), t.getReviewCount(), favoriteRepository.countByTrainerId(t.getId()), next);
    }

    private String uniqueId(String base) {
        String id = base;
        int i = 2;
        while (trainerRepository.existsById(id)) id = base + "-" + i++;
        return id;
    }
}

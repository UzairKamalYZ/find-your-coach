package com.findmycoach.trainer;

import com.findmycoach.booking.BookingRepository;
import com.findmycoach.booking.BookingStatus;
import com.findmycoach.common.Slots;
import com.findmycoach.common.Strings;
import com.findmycoach.common.exception.ApiException;
import com.findmycoach.common.web.PageResponse;
import com.findmycoach.common.web.Pagination;
import com.findmycoach.config.AppProperties;
import com.findmycoach.trainer.dto.DayAvailability;
import com.findmycoach.trainer.dto.DayAvailability.SlotAvailability;
import com.findmycoach.trainer.dto.DayAvailability.SlotStatus;
import com.findmycoach.trainer.dto.TrainerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainerService {

    private static final Set<BookingStatus> SLOT_HOLDING = EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED, BookingStatus.NO_SHOW);

    private final TrainerRepository trainerRepository;
    private final TimeOffRepository timeOffRepository;
    private final BookingRepository bookingRepository;
    private final AppProperties props;
    private final Clock clock;

    public record SearchCriteria(String sport, String city, String q, BigDecimal minRate, BigDecimal maxRate,
                                 Double minRating, Boolean featured, String sort) {
    }

    @Transactional(readOnly = true)
    public PageResponse<TrainerResponse> search(SearchCriteria c, Collection<TrainerStatus> statuses, int page, int size) {
        var pageable = Pagination.of(page, size, sortFor(c.sort()));
        var result = trainerRepository.search(statuses, Strings.clean(c.sport()), Strings.clean(c.city()),
                Strings.clean(c.q()), c.minRate(), c.maxRate(), c.minRating(), c.featured(), pageable);
        return PageResponse.of(result, TrainerResponse::from);
    }

    static Sort sortFor(String sort) {
        if (sort == null) sort = "recommended";
        return switch (sort) {
            case "price_asc" -> Sort.by("hourlyRate").ascending().and(Sort.by("name"));
            case "price_desc" -> Sort.by("hourlyRate").descending().and(Sort.by("name"));
            case "rating" -> Sort.by("rating").descending().and(Sort.by("reviewCount").descending());
            case "reviews" -> Sort.by("reviewCount").descending();
            case "experience" -> Sort.by("experience").descending();
            case "name" -> Sort.by("name").ascending();
            case "newest" -> Sort.by("createdAt").descending();
            default -> Sort.by(Sort.Order.desc("featured"), Sort.Order.desc("rating"), Sort.Order.desc("reviewCount"), Sort.Order.asc("name"));
        };
    }

    @Transactional(readOnly = true)
    public Trainer require(String id) {
        return trainerRepository.findById(id).orElseThrow(() -> ApiException.notFound("Coach not found"));
    }

    /** Public view: only ACTIVE listings are visible. */
    @Transactional(readOnly = true)
    public TrainerResponse getPublic(String id) {
        Trainer t = require(id);
        if (t.getStatus() != TrainerStatus.ACTIVE) throw ApiException.notFound("Coach not found");
        return TrainerResponse.from(t);
    }

    @Transactional(readOnly = true)
    public List<String> activeCities() {
        return trainerRepository.findActiveCities();
    }

    @Transactional(readOnly = true)
    public List<DayAvailability> availability(String trainerId, LocalDate from, LocalDate to) {
        Trainer t = require(trainerId);
        LocalDate today = LocalDate.now(clock);
        if (from == null) from = today;
        if (to == null) to = from.plusDays(13);
        if (to.isBefore(from)) throw ApiException.badRequest("'to' must not be before 'from'");
        long days = Duration.between(from.atStartOfDay(), to.atStartOfDay()).toDays() + 1;
        if (days > props.booking().maxAvailabilityRangeDays()) {
            throw ApiException.badRequest("Date range must be at most " + props.booking().maxAvailabilityRangeDays() + " days");
        }
        boolean bookable = t.isBookable();
        LocalDate lastBookable = today.plusDays(props.booking().bookingWindowDays() - 1L);

        Map<DayOfWeek, Set<String>> weekly = t.getWeeklySlots().stream().collect(Collectors.groupingBy(
                WeeklySlot::getDayOfWeek, () -> new EnumMap<>(DayOfWeek.class),
                Collectors.mapping(WeeklySlot::getSlot, Collectors.toSet())));
        Map<LocalDate, String> timeOff = timeOffRepository.findByTrainerIdAndDateBetweenOrderByDateAsc(trainerId, from, to)
                .stream().collect(Collectors.toMap(TimeOff::getDate, o -> o.getReason() == null ? "" : o.getReason(), (a, b) -> a));
        Map<LocalDate, Set<String>> booked = new HashMap<>();
        bookingRepository.findByTrainerIdAndDateBetweenAndStatusIn(trainerId, from, to, SLOT_HOLDING)
                .forEach(b -> booked.computeIfAbsent(b.getDate(), d -> new HashSet<>()).add(b.getSlot()));

        LocalTime now = LocalTime.now(clock);
        List<DayAvailability> out = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            boolean off = timeOff.containsKey(d);
            Set<String> working = weekly.getOrDefault(d.getDayOfWeek(), Set.of());
            Set<String> taken = booked.getOrDefault(d, Set.of());
            List<SlotAvailability> slots = new ArrayList<>();
            for (String s : Slots.ALL) {
                SlotStatus st;
                boolean past = d.isBefore(today) || (d.equals(today) && !Slots.toTime(s).isAfter(now));
                if (taken.contains(s)) st = SlotStatus.BOOKED;
                else if (past) st = SlotStatus.PAST;
                else if (!bookable || off || !working.contains(s) || d.isAfter(lastBookable)) st = SlotStatus.UNAVAILABLE;
                else st = SlotStatus.AVAILABLE;
                slots.add(new SlotAvailability(s, st));
            }
            out.add(new DayAvailability(d, off, off ? Strings.clean(timeOff.get(d)) : null, slots));
        }
        return out;
    }

    /** True if the coach normally works this slot and is not on time off that day. */
    @Transactional(readOnly = true)
    public boolean worksAt(Trainer t, LocalDate date, String slot) {
        boolean inSchedule = t.getWeeklySlots().contains(new WeeklySlot(date.getDayOfWeek(), slot));
        return inSchedule && !timeOffRepository.existsByTrainerIdAndDate(t.getId(), date);
    }

    public static Set<WeeklySlot> defaultSchedule() {
        Set<WeeklySlot> s = new HashSet<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            if (d == DayOfWeek.SUNDAY) continue;
            for (String slot : Slots.ALL) s.add(new WeeklySlot(d, slot));
        }
        return s;
    }
}

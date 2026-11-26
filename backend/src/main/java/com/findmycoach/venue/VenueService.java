package com.findmycoach.venue;

import com.findmycoach.audit.AuditService;
import com.findmycoach.common.Slugs;
import com.findmycoach.common.Strings;
import com.findmycoach.common.exception.ApiException;
import com.findmycoach.common.web.PageResponse;
import com.findmycoach.common.web.Pagination;
import com.findmycoach.config.AppProperties;
import com.findmycoach.security.AuthUser;
import com.findmycoach.sport.SportService;
import com.findmycoach.venue.dto.VenueDayAvailability;
import com.findmycoach.venue.dto.VenueRequest;
import com.findmycoach.venue.dto.VenueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VenueService {
    private final VenueRepository venueRepository;
    private final VenueBookingRepository venueBookingRepository;
    private final SportService sportService;
    private final AuditService auditService;
    private final AppProperties props;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<VenueResponse> search(String sport, String city, VenueType type, String q, BigDecimal maxPrice,
                                              String sort, boolean includeInactive, int page, int size) {
        Sort s = switch (sort == null ? "" : sort) {
            case "price_asc" -> Sort.by("pricePerHour").ascending();
            case "price_desc" -> Sort.by("pricePerHour").descending();
            case "name" -> Sort.by("name").ascending();
            default -> Sort.by("rating").descending().and(Sort.by("name"));
        };
        var result = venueRepository.search(Strings.clean(sport), Strings.clean(city), type, Strings.clean(q), maxPrice,
                includeInactive, Pagination.of(page, size, s));
        return PageResponse.of(result, VenueResponse::from);
    }

    @Transactional(readOnly = true)
    public Venue require(String id) {
        return venueRepository.findById(id).orElseThrow(() -> ApiException.notFound("Venue not found"));
    }

    @Transactional(readOnly = true)
    public VenueResponse getPublic(String id) {
        Venue v = require(id);
        if (!v.isActive()) throw ApiException.notFound("Venue not found");
        return VenueResponse.from(v);
    }

    @Transactional(readOnly = true)
    public List<VenueDayAvailability> availability(String id, LocalDate from, LocalDate to) {
        Venue v = require(id);
        LocalDate today = LocalDate.now(clock);
        if (from == null) from = today;
        if (to == null) to = from.plusDays(6);
        if (to.isBefore(from)) throw ApiException.badRequest("'to' must not be before 'from'");
        if (Duration.between(from.atStartOfDay(), to.atStartOfDay()).toDays() + 1 > props.booking().maxAvailabilityRangeDays()) {
            throw ApiException.badRequest("Date range must be at most " + props.booking().maxAvailabilityRangeDays() + " days");
        }
        List<VenueBooking> bookings = venueBookingRepository.findByVenueIdAndDateBetweenAndStatusIn(id, from, to,
                EnumSet.of(VenueBookingStatus.CONFIRMED, VenueBookingStatus.COMPLETED));
        LocalDateTime now = LocalDateTime.now(clock);
        List<VenueDayAvailability> out = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            final LocalDate day = d;
            List<VenueDayAvailability.HourAvailability> hours = new ArrayList<>();
            for (LocalTime t = v.getOpenTime(); t.isBefore(v.getCloseTime()); t = t.plusHours(1)) {
                final LocalTime hour = t;
                long used = bookings.stream().filter(b -> b.getDate().equals(day) && b.overlaps(hour, 1)).count();
                int remaining = (int) Math.max(0, v.getCapacity() - used);
                boolean future = day.atTime(hour).isAfter(now);
                hours.add(new VenueDayAvailability.HourAvailability(hour.toString(), remaining,
                        v.isActive() && future && remaining > 0));
                if (t.equals(LocalTime.of(23, 0))) break;
            }
            out.add(new VenueDayAvailability(day, hours));
        }
        return out;
    }

    // ------------------------------------------------------------ admin

    @Transactional
    public VenueResponse create(AuthUser admin, VenueRequest req) {
        String id = Strings.isBlank(req.id()) ? uniqueId(Slugs.slugify(req.name())) : req.id();
        if (venueRepository.existsById(id)) throw ApiException.conflict("A venue with id '" + id + "' already exists");
        Venue v = new Venue();
        v.setId(id);
        apply(v, req);
        venueRepository.save(v);
        auditService.record(admin, "VENUE_CREATED", "VENUE", id, v.getName());
        return VenueResponse.from(v);
    }

    @Transactional
    public VenueResponse update(AuthUser admin, String id, VenueRequest req) {
        Venue v = require(id);
        apply(v, req);
        auditService.record(admin, "VENUE_UPDATED", "VENUE", id, v.getName());
        return VenueResponse.from(v);
    }

    @Transactional
    public void deactivate(AuthUser admin, String id) {
        Venue v = require(id);
        v.setActive(false);
        auditService.record(admin, "VENUE_DEACTIVATED", "VENUE", id, v.getName());
    }

    private void apply(Venue v, VenueRequest r) {
        v.setName(r.name().trim());
        v.setType(r.type());
        v.setSport(sportService.requireActive(r.sport()));
        v.setCity(r.city().trim());
        v.setAddress(Strings.clean(r.address()));
        v.setDescription(Strings.clean(r.description()));
        v.setPricePerHour(r.pricePerHour());
        if (r.rating() != null) v.setRating(r.rating());
        v.setImage(Strings.clean(r.image()));
        v.setAmenities(r.amenities() == null ? new ArrayList<>() : new ArrayList<>(r.amenities()));
        if (r.capacity() != null) v.setCapacity(r.capacity());
        if (r.openTime() != null) v.setOpenTime(r.openTime().withMinute(0).withSecond(0).withNano(0));
        if (r.closeTime() != null) v.setCloseTime(r.closeTime().withMinute(0).withSecond(0).withNano(0));
        if (!v.getOpenTime().isBefore(v.getCloseTime())) throw ApiException.badRequest("openTime must be before closeTime");
        if (r.active() != null) v.setActive(r.active());
    }

    private String uniqueId(String base) {
        String id = base;
        int i = 2;
        while (venueRepository.existsById(id)) id = base + "-" + i++;
        return id;
    }
}

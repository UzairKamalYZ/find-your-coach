package com.findmycoach.seed;

import com.findmycoach.booking.Booking;
import com.findmycoach.booking.BookingRepository;
import com.findmycoach.booking.BookingStatus;
import com.findmycoach.common.Slots;
import com.findmycoach.config.AppProperties;
import com.findmycoach.review.Review;
import com.findmycoach.review.ReviewRepository;
import com.findmycoach.sport.Sport;
import com.findmycoach.sport.SportRepository;
import com.findmycoach.trainer.Trainer;
import com.findmycoach.trainer.TrainerRepository;
import com.findmycoach.trainer.TrainerStatus;
import com.findmycoach.trainer.WeeklySlot;
import com.findmycoach.user.AuthProvider;
import com.findmycoach.user.Role;
import com.findmycoach.user.User;
import com.findmycoach.user.UserRepository;
import com.findmycoach.venue.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/**
 * Seeds the catalogue from the original frontend mock data plus demo accounts for each role.
 * Runs only on an empty database and only when app.seed-demo-data=true.
 *
 * Demo accounts (password in brackets):
 *   admin@findmytrainer.dev  (Admin@12345)   – ADMIN
 *   sam@findmytrainer.dev    (Trainee@123)   – TRAINEE with upcoming and past sessions
 *   marcus@findmytrainer.dev (Trainer@123)   – TRAINER owning the "marcus-reed" listing (also sofia@, arjun@, leah@)
 *   hannah@findmytrainer.dev (Trainer@123)   – TRAINER whose new listing awaits admin approval
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    public static final String ADMIN_EMAIL = "admin@findmytrainer.dev";
    public static final String ADMIN_PASSWORD = "Admin@12345";
    public static final String TRAINEE_EMAIL = "sam@findmytrainer.dev";
    public static final String TRAINEE_PASSWORD = "Trainee@123";
    public static final String TRAINER_PASSWORD = "Trainer@123";

    private final AppProperties props;
    private final UserRepository userRepository;
    private final SportRepository sportRepository;
    private final TrainerRepository trainerRepository;
    private final VenueRepository venueRepository;
    private final VenueBookingRepository venueBookingRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (sportRepository.count() == 0) seedSports();
        if (!props.seedDemoData() || userRepository.count() > 0) return;
        log.info("Seeding demo data");

        String trainerHash = passwordEncoder.encode(TRAINER_PASSWORD);
        user(ADMIN_EMAIL, passwordEncoder.encode(ADMIN_PASSWORD), "Avery Admin", "London", Role.ADMIN, null);
        User sam = user(TRAINEE_EMAIL, passwordEncoder.encode(TRAINEE_PASSWORD), "Sam Carter", "London", Role.TRAINEE, "+44 7700 900123");
        sam.setSports("Tennis, Basketball");
        sam.setLevel("Intermediate");
        sam.setGoals("Improve my serve and get match fit");
        User priya = user("priya@findmytrainer.dev", passwordEncoder.encode(TRAINEE_PASSWORD), "Priya Shah", "Manchester", Role.TRAINEE, null);
        User tom = user("tom@findmytrainer.dev", passwordEncoder.encode(TRAINEE_PASSWORD), "Tom Baker", "Birmingham", Role.TRAINEE, null);

        Trainer marcus = trainer("marcus-reed", user("marcus@findmytrainer.dev", trainerHash, "Marcus Reed", "London", Role.TRAINER, "+44 7700 900201"),
                "Tennis", "London", 4.9, 184, 65, 12,
                "Former ATP qualifier turned coach. I help juniors and adults sharpen technique, footwork and match strategy.",
                "trainer-1", List.of("Serve & Volley", "Junior Development", "Match Play"), "LTA Level 5 Performance Coach",
                schedule(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY), except("12:00")));
        Trainer sofia = trainer("sofia-alvarez", user("sofia@findmytrainer.dev", trainerHash, "Sofia Alvarez", "Manchester", Role.TRAINER, null),
                "Football", "Manchester", 4.8, 142, 55, 9,
                "UEFA B licensed coach. Focused on positional play, finishing and game intelligence for ages 8 to adult.",
                "trainer-2", List.of("Finishing", "1v1 Defending", "Position Play"), "UEFA B Licence",
                merge(schedule(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), List.of("15:00", "17:00", "18:00", "19:00")),
                        schedule(EnumSet.of(DayOfWeek.SATURDAY), List.of("08:00", "09:00", "10:00", "12:00"))));
        Trainer arjun = trainer("arjun-mehta", user("arjun@findmytrainer.dev", trainerHash, "Arjun Mehta", "Birmingham", Role.TRAINER, null),
                "Cricket", "Birmingham", 4.9, 98, 50, 14,
                "Ex-county batsman. Specializing in batting fundamentals, spin play, and high-pressure scenario training.",
                "trainer-3", List.of("Batting", "Spin Play", "Fielding Drills"), "ECB Level 3 Coach",
                schedule(EnumSet.complementOf(EnumSet.of(DayOfWeek.MONDAY)), List.of("08:00", "09:00", "10:00", "12:00", "14:00", "15:00")));
        Trainer leah = trainer("leah-okafor", user("leah@findmytrainer.dev", trainerHash, "Leah Okafor", "London", Role.TRAINER, null),
                "Basketball", "London", 4.7, 121, 60, 8,
                "Pro shooting coach. Build a deadly jumper, tighten your handle, and develop pro-level court IQ.",
                "trainer-4", List.of("Shooting", "Ball Handling", "Conditioning"), "Basketball England Level 2",
                schedule(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY), except("08:00")));
        Trainer hannah = trainer("hannah-cole", user("hannah@findmytrainer.dev", trainerHash, "Hannah Cole", "Leeds", Role.TRAINER, null),
                "Badminton", "Leeds", 0, 0, 40, 6,
                "County badminton player coaching footwork, deception and doubles rotation for club players.",
                null, List.of("Footwork", "Doubles Tactics"), "Badminton England Level 2",
                schedule(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), List.of("17:00", "18:00", "19:00")));
        hannah.setStatus(TrainerStatus.PENDING);
        hannah.setFeatured(false);

        Venue riverside = venue("v1", "Riverside Tennis Club", VenueType.COURT, "Tennis", "London", "12 Embankment Walk, London SE1",
                30, 4.7, "venue-tennis", 4, List.of("Floodlights", "Changing rooms", "Racket hire", "Café"),
                "Four floodlit hard courts on the river with racket hire and a members' café.");
        venue("v2", "Northgate Football Arena", VenueType.PITCH, "Football", "Manchester", "Northgate Retail Park, Manchester M4",
                80, 4.8, "venue-football", 2, List.of("3G turf", "Floodlights", "Changing rooms", "Parking"),
                "Two full-size 3G pitches that split into 5-a-side or 7-a-side.");
        venue("v3", "Hardwood Hoops Center", VenueType.COURT, "Basketball", "London", "88 Mile End Road, London E1",
                45, 4.6, "venue-basketball", 2, List.of("Indoor", "Scoreboard", "Showers"),
                "Indoor sprung hardwood courts with electronic scoreboards.");
        venue("v4", "Royal Cricket Ground", VenueType.STADIUM, "Cricket", "Birmingham", "Pavilion Lane, Birmingham B5",
                120, 4.9, "venue-cricket", 1, List.of("Nets", "Bowling machine", "Pavilion", "Parking"),
                "Historic ground with turf nets and a bowling machine available on request.");

        LocalDate today = LocalDate.now(clock);
        // Sam's history: a reviewed past session, a completed session awaiting review, and two upcoming sessions.
        Booking pastReviewed = booking(sam, marcus, today.minusDays(12), "09:00", BookingStatus.COMPLETED);
        booking(sam, leah, today.minusDays(3), "17:00", BookingStatus.COMPLETED);
        booking(sam, marcus, nextWorkingDay(marcus, today.plusDays(2), "10:00"), "10:00", BookingStatus.CONFIRMED);
        booking(sam, leah, nextWorkingDay(leah, today.plusDays(5), "18:00"), "18:00", BookingStatus.CONFIRMED);
        // Other trainees keep the coaches' calendars realistic.
        booking(priya, sofia, nextWorkingDay(sofia, today.plusDays(1), "17:00"), "17:00", BookingStatus.CONFIRMED);
        booking(priya, sofia, today.minusDays(6), "18:00", BookingStatus.COMPLETED);
        booking(tom, arjun, nextWorkingDay(arjun, today.plusDays(1), "09:00"), "09:00", BookingStatus.CONFIRMED);
        booking(tom, marcus, nextWorkingDay(marcus, today.plusDays(1), "14:00"), "14:00", BookingStatus.CONFIRMED);

        Review r = new Review();
        r.setBooking(pastReviewed);
        r.setTrainer(marcus);
        r.setAuthor(sam);
        r.setRating(5);
        r.setComment("Marcus completely rebuilt my second serve in one session. Clear, patient and fun.");
        r.setReply("Thanks Sam — keep up the shadow swings!");
        r.setRepliedAt(clock.instant());
        r.setCreatedAt(clock.instant().minus(Duration.ofDays(11)));
        reviewRepository.save(r); // part of the imported baseline, totals are not changed
        historicalReview(sofia, priya, 5, "Great drills for finishing, my weak foot is finally useful.", 20);
        historicalReview(arjun, tom, 5, "Arjun reads your technique instantly. Best coaching I've had.", 30);
        historicalReview(leah, sam, 4, "Intense but worth it — shooting form already feels smoother.", 40);

        VenueBooking vb = new VenueBooking();
        vb.setVenue(riverside);
        vb.setUser(sam);
        vb.setDate(today.plusDays(3));
        vb.setStartTime(LocalTime.of(18, 0));
        vb.setHours(1);
        vb.setTotalPrice(riverside.getPricePerHour());
        vb.setNotes("Practice after lesson");
        venueBookingRepository.save(vb);
        log.info("Demo data ready: admin={} trainee={} trainers=marcus@/sofia@/arjun@/leah@/hannah@findmytrainer.dev",
                ADMIN_EMAIL, TRAINEE_EMAIL);
    }

    private void seedSports() {
        String[][] sports = {{"Tennis", "🎾"}, {"Football", "⚽"}, {"Cricket", "🏏"}, {"Basketball", "🏀"}, {"Badminton", "🏸"}, {"Swimming", "🏊"}};
        for (int i = 0; i < sports.length; i++) sportRepository.save(new Sport(sports[i][0], sports[i][1], i));
    }

    private User user(String email, String hash, String name, String city, Role role, String phone) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(hash);
        u.setName(name);
        u.setCity(city);
        u.setRole(role);
        u.setPhone(phone);
        u.setProvider(AuthProvider.EMAIL);
        return userRepository.save(u);
    }

    private Trainer trainer(String id, User owner, String sport, String city, double rating, int reviews, int rate, int exp,
                            String bio, String image, List<String> specialties, String qualifications, Set<WeeklySlot> slots) {
        Trainer t = new Trainer();
        t.setId(id);
        t.setUser(owner);
        t.setName(owner.getName());
        t.setSport(sport);
        t.setCity(city);
        t.setRatingTotal(rating * reviews);
        t.setReviewCount(reviews);
        t.recalcRating();
        t.setHourlyRate(BigDecimal.valueOf(rate).setScale(2));
        t.setExperience(exp);
        t.setBio(bio);
        t.setImage(image);
        t.setSpecialties(new ArrayList<>(specialties));
        t.setQualifications(qualifications);
        t.setStatus(TrainerStatus.ACTIVE);
        t.setFeatured(true);
        t.setWeeklySlots(slots);
        owner.setSports(sport);
        owner.setExperience(String.valueOf(exp));
        owner.setQualifications(qualifications);
        owner.setBio(bio);
        return trainerRepository.save(t);
    }

    private Venue venue(String id, String name, VenueType type, String sport, String city, String address, int price,
                        double rating, String image, int capacity, List<String> amenities, String description) {
        Venue v = new Venue();
        v.setId(id);
        v.setName(name);
        v.setType(type);
        v.setSport(sport);
        v.setCity(city);
        v.setAddress(address);
        v.setPricePerHour(BigDecimal.valueOf(price).setScale(2));
        v.setRating(rating);
        v.setImage(image);
        v.setCapacity(capacity);
        v.setAmenities(new ArrayList<>(amenities));
        v.setDescription(description);
        return venueRepository.save(v);
    }

    private Booking booking(User trainee, Trainer t, LocalDate date, String slot, BookingStatus status) {
        Booking b = new Booking();
        b.setTrainee(trainee);
        b.setTrainer(t);
        b.setTrainerName(t.getName());
        b.setTrainerImage(t.getImage());
        b.setSport(t.getSport());
        b.setDate(date);
        b.setSlot(slot);
        b.setPrice(t.getHourlyRate());
        b.setStatus(status);
        return bookingRepository.save(b);
    }

    private void historicalReview(Trainer t, User author, int rating, String comment, int daysAgo) {
        Review r = new Review();
        r.setTrainer(t);
        r.setAuthor(author);
        r.setRating(rating);
        r.setComment(comment);
        r.setCreatedAt(clock.instant().minus(Duration.ofDays(daysAgo)));
        reviewRepository.save(r);
    }

    private static LocalDate nextWorkingDay(Trainer t, LocalDate from, String slot) {
        LocalDate d = from;
        for (int i = 0; i < 14; i++, d = d.plusDays(1)) {
            if (t.getWeeklySlots().contains(new WeeklySlot(d.getDayOfWeek(), slot))) return d;
        }
        return from;
    }

    private static List<String> except(String... slots) {
        List<String> l = new ArrayList<>(Slots.ALL);
        l.removeAll(List.of(slots));
        return l;
    }

    private static Set<WeeklySlot> schedule(Set<DayOfWeek> days, List<String> slots) {
        Set<WeeklySlot> s = new HashSet<>();
        for (DayOfWeek d : days) for (String slot : slots) s.add(new WeeklySlot(d, slot));
        return s;
    }

    @SafeVarargs
    private static Set<WeeklySlot> merge(Set<WeeklySlot>... sets) {
        Set<WeeklySlot> s = new HashSet<>();
        for (Set<WeeklySlot> x : sets) s.addAll(x);
        return s;
    }
}

package com.findmycoach.common.seed;

import com.findmycoach.trainer.Trainer;
import com.findmycoach.trainer.TrainerRepository;
import com.findmycoach.venue.Venue;
import com.findmycoach.venue.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@org.springframework.context.annotation.Profile("!test")
public class DataSeeder implements ApplicationRunner {

    private final TrainerRepository trainerRepository;
    private final VenueRepository venueRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createPartialUniqueIndex();
        if (trainerRepository.count() == 0) {
            seedTrainers();
            seedVenues();
            log.info("Seeded trainers and venues");
        }
    }

    private void createPartialUniqueIndex() {
        jdbcTemplate.execute("""
            CREATE UNIQUE INDEX IF NOT EXISTS uq_trainer_slot_confirmed
            ON bookings (trainer_id, booking_date, slot_time)
            WHERE status = 'CONFIRMED'
            """);
    }

    private void seedTrainers() {
        trainerRepository.saveAll(List.of(
            trainer("marcus-reed", "Marcus Reed", "Tennis", "London",
                BigDecimal.valueOf(4.9), 184, 6500, 12,
                "Former ATP qualifier turned coach. I help juniors and adults sharpen technique, footwork and match strategy.",
                "[\"Serve & Volley\",\"Junior Development\",\"Match Play\"]"),
            trainer("sofia-alvarez", "Sofia Alvarez", "Football", "Manchester",
                BigDecimal.valueOf(4.8), 142, 5500, 9,
                "UEFA B licensed coach. Focused on positional play, finishing and game intelligence for ages 8 to adult.",
                "[\"Finishing\",\"1v1 Defending\",\"Position Play\"]"),
            trainer("arjun-mehta", "Arjun Mehta", "Cricket", "Birmingham",
                BigDecimal.valueOf(4.9), 98, 5000, 14,
                "Ex-county batsman. Specializing in batting fundamentals, spin play, and high-pressure scenario training.",
                "[\"Batting\",\"Spin Play\",\"Fielding Drills\"]"),
            trainer("leah-okafor", "Leah Okafor", "Basketball", "London",
                BigDecimal.valueOf(4.7), 121, 6000, 8,
                "Pro shooting coach. Build a deadly jumper, tighten your handle, and develop pro-level court IQ.",
                "[\"Shooting\",\"Ball Handling\",\"Conditioning\"]")
        ));
    }

    private void seedVenues() {
        venueRepository.saveAll(List.of(
            venue("v1", "Riverside Tennis Club", "Court", "Tennis", "London", 3000, BigDecimal.valueOf(4.7)),
            venue("v2", "Northgate Football Arena", "Pitch", "Football", "Manchester", 8000, BigDecimal.valueOf(4.8)),
            venue("v3", "Hardwood Hoops Center", "Court", "Basketball", "London", 4500, BigDecimal.valueOf(4.6)),
            venue("v4", "Royal Cricket Ground", "Stadium", "Cricket", "Birmingham", 12000, BigDecimal.valueOf(4.9))
        ));
    }

    private Trainer trainer(String id, String name, String sport, String city,
                            BigDecimal rating, int reviews, int hourlyRate,
                            int experience, String bio, String specialties) {
        Trainer t = new Trainer();
        t.setId(id); t.setName(name); t.setSport(sport); t.setCity(city);
        t.setRating(rating); t.setReviews(reviews); t.setHourlyRate(hourlyRate);
        t.setExperience(experience); t.setBio(bio); t.setSpecialties(specialties);
        return t;
    }

    private Venue venue(String id, String name, String type, String sport,
                        String city, int pricePerHour, BigDecimal rating) {
        Venue v = new Venue();
        v.setId(id); v.setName(name); v.setType(type); v.setSport(sport);
        v.setCity(city); v.setPricePerHour(pricePerHour); v.setRating(rating);
        return v;
    }
}

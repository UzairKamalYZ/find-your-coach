package com.findmycoach.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        List<String> corsAllowedOrigins,
        String timeZone,
        boolean seedDemoData,
        Jwt jwt,
        Booking booking
) {
    public record Jwt(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {
    }

    public record Booking(int bookingWindowDays, int maxAvailabilityRangeDays, int freeCancellationHours) {
    }
}

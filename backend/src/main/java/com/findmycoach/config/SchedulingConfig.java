package com.findmycoach.config;

import com.findmycoach.auth.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class SchedulingConfig {
    private final RefreshTokenService refreshTokenService;

    @Scheduled(cron = "0 30 3 * * *")
    public void purgeExpiredRefreshTokens() {
        refreshTokenService.purgeExpired();
    }
}

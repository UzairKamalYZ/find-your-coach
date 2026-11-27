package com.findmycoach.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    /** Business clock in the configured time zone. Tests can replace this bean to freeze time. */
    @Bean
    public Clock clock(AppProperties props) {
        return Clock.system(ZoneId.of(props.timeZone()));
    }
}

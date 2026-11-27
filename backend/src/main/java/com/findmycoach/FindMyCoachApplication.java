package com.findmycoach;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FindMyCoachApplication {
    public static void main(String[] args) {
        SpringApplication.run(FindMyCoachApplication.class, args);
    }
}

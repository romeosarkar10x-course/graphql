package dev.shelfie.zoo;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ZooApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZooApplication.class, args);
    }

    /** The real clock, or a fixed one when shelfie.clock is set: tests decide what day it is. */
    @Bean
    Clock clock(@Value("${shelfie.clock:}") String fixed, @Value("${shelfie.zone}") String zone) {
        return fixed.isBlank() ? Clock.system(ZoneId.of(zone)) : Clock.fixed(Instant.parse(fixed), ZoneId.of(zone));
    }
}

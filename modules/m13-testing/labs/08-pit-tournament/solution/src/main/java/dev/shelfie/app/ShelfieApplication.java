package dev.shelfie.app;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Shelfie under test (Module 13): graph/ (controllers) calls service/ (rules), which calls store/ (data). */
@SpringBootApplication
public class ShelfieApplication {

    public static void main(String[] args) { SpringApplication.run(ShelfieApplication.class, args); }

    /** A fixed clock, so new reviews get reproducible timestamps. Tests may replace it. */
    @Bean
    Clock clock() { return Clock.fixed(Instant.parse("2026-11-21T12:00:00Z"), ZoneOffset.UTC); }
}

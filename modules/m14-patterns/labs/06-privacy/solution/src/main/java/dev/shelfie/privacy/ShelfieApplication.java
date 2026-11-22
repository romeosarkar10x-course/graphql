package dev.shelfie.privacy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShelfieApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShelfieApplication.class, args);
    }
}

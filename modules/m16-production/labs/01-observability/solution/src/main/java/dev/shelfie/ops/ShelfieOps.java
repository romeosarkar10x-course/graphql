package dev.shelfie.ops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Shelfie in production (Module 16). java -jar target/shelfie-ops.jar; the reviews service must be on :4100. */
@SpringBootApplication
public class ShelfieOps {
    public static void main(String[] args) {
        SpringApplication.run(ShelfieOps.class, args);
    }
}

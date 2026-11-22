package dev.shelfie.digest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Lab 12.2: a second Spring service that is a GraphQL client of Shelfie and of the chat server. */
@SpringBootApplication
public class DigestApplication {
  public static void main(String[] args) {
    SpringApplication.run(DigestApplication.class, args);
  }
}

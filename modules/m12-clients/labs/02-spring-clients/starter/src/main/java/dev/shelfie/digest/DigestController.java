package dev.shelfie.digest;

import dev.shelfie.digest.Model.*;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Lab 12.2: every endpoint here is a GraphQL call to Shelfie (digest.shelfie-graphql) or reads the chat
 * subscription (digest.chat, room digest.room). Build the clients as beans, put the documents in
 * src/main/resources/graphql-documents/, and replace each 501 below.
 */
@RestController
public class DigestController {

  /** The club digest (README for the fields) through HttpSyncGraphQlClient; 404 for an unknown club. */
  @GetMapping("/clubs/{id}")
  ResponseEntity<ClubDigest> club(@PathVariable String id) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  /** The same through HttpGraphQlClient, returning a Mono. */
  @GetMapping("/clubs/{id}/reactive")
  ResponseEntity<ClubDigest> clubReactive(@PathVariable String id) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  /** createReview as the user in X-User (Shelfie reads X-Shelfie-User): 201, or 422 with the userErrors. */
  @PostMapping("/books/{id}/reviews")
  ResponseEntity<ReviewPayload> review(@PathVariable String id, @RequestHeader("X-User") String user, @RequestBody NewReview body) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
  }

  /** The last 20 messages of the subscribed room, oldest first. */
  @GetMapping("/chat")
  List<Message> chat() {
    return List.of();
  }

  /** Requests per operation name, counted by a GraphQL client interceptor: {"clubDigest":{"requests":2,...}}. */
  @GetMapping("/stats")
  Map<String, Object> stats() {
    return Map.of();
  }
}

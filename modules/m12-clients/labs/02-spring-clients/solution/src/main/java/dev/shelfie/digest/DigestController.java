package dev.shelfie.digest;

import dev.shelfie.digest.Model.*;
import java.util.Map;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
public class DigestController {
  private final HttpSyncGraphQlClient sync;
  private final HttpGraphQlClient reactive;
  private final OperationStats stats;

  DigestController(HttpSyncGraphQlClient sync, HttpGraphQlClient reactive, OperationStats stats) {
    this.sync = sync;
    this.reactive = reactive;
    this.stats = stats;
  }

  /**
   * Blocking: retrieveSync(path).toEntity(type). A null club decodes to null. documentName loads the file but
   * does not send an operationName; set it, so Shelfie logs and the interceptor know the operation.
   */
  @GetMapping("/clubs/{id}")
  ResponseEntity<ClubDigest> club(@PathVariable String id) {
    ClubDigest club = sync.documentName("clubDigest").operationName("clubDigest").variable("id", id).retrieveSync("club").toEntity(ClubDigest.class);
    return club == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(club);
  }

  /** The same document through WebClient: a Mono, no thread waits for Shelfie. */
  @GetMapping("/clubs/{id}/reactive")
  Mono<ResponseEntity<ClubDigest>> clubReactive(@PathVariable String id) {
    return reactive.documentName("clubDigest").operationName("clubDigest").variable("id", id).retrieve("club").toEntity(ClubDigest.class)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  /** The caller's user becomes Shelfie's X-Shelfie-User header: a per-request copy of the client (mutate). */
  @PostMapping("/books/{id}/reviews")
  ResponseEntity<ReviewPayload> review(@PathVariable String id, @RequestHeader("X-User") String user, @RequestBody NewReview body) {
    ReviewPayload payload = sync.mutate().header("X-Shelfie-User", user).build()
        .documentName("createReview").operationName("createReview")
        .variable("input", Map.of("bookId", id, "rating", body.rating(), "body", body.body()))
        .retrieveSync("createReview").toEntity(ReviewPayload.class);
    HttpStatus status = payload.userErrors().isEmpty() ? HttpStatus.CREATED : HttpStatus.UNPROCESSABLE_CONTENT;
    return ResponseEntity.status(status).body(payload);
  }

  @GetMapping("/stats")
  Map<String, Object> stats() {
    return stats.snapshot();
  }
}

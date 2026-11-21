package dev.shelfie.live;

import java.time.Duration;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/** Course-only endpoints for lab 11.3's experiment. Not part of Shelfie's API. */
@RestController
class CourseController {
  private final Bus bus;

  CourseController(Bus bus) { this.bus = bus; }

  /** Typing events from `authors` bots (bot1, bot2, ...) at `perSecond` in total, for `seconds`. Returns at once. */
  @PostMapping("/_course/flood")
  Map<String, Object> flood(@RequestParam String roomId, @RequestParam(defaultValue = "10000") int perSecond,
                            @RequestParam(defaultValue = "3") int seconds, @RequestParam(defaultValue = "3") int authors) {
    int perMs = Math.max(1, perSecond / 1000);
    Flux.interval(Duration.ofMillis(1)).take(seconds * 1000L).onBackpressureDrop()
        .subscribe(t -> { for (int i = 0; i < perMs; i++) bus.typing(new Typing(roomId, "bot" + (1 + (t * perMs + i) % authors))); });
    return Map.of("roomId", roomId, "events", (long) perMs * seconds * 1000);
  }
}

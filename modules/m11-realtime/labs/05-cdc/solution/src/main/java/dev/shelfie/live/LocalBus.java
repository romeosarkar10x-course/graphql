package dev.shelfie.live;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/** One JVM's bus: a Reactor sink per event kind. */
@Component
public class LocalBus implements Bus {
  // directBestEffort: a subscriber that cannot keep up misses events instead of stalling the others.
  private final Sinks.Many<Message> messages = Sinks.many().multicast().directBestEffort();
  private final Sinks.Many<Typing> typing = Sinks.many().multicast().directBestEffort();
  private final Sinks.Many<String> archived = Sinks.many().multicast().directBestEffort();
  private final Map<String, AtomicInteger> watching = new ConcurrentHashMap<>();
  private static final Sinks.EmitFailureHandler RETRY = Sinks.EmitFailureHandler.busyLooping(Duration.ofMillis(50));

  @Override
  public void publish(Message m) { messages.emitNext(m, RETRY); }

  @Override
  public Flux<Message> messages(String roomId) {
    return watched(roomId, messages.asFlux().filter(m -> m.roomId().equals(roomId)));
  }

  @Override
  public void typing(Typing t) { typing.emitNext(t, RETRY); }

  // Lecture 11.3: a typing indicator only needs "who is typing now". Each 250 ms window passes each author
  // once, so 10,000 events a second become at most 4 per author per second, and a slow client never
  // queues up stale ones. The subscriber's own events are filtered out per subscription.
  @Override
  public Flux<Typing> typing(String roomId, String me) {
    return watched(roomId, typing.asFlux()
        .filter(t -> t.roomId().equals(roomId) && !t.author().equals(me))
        .window(Duration.ofMillis(250))
        .concatMap(w -> w.distinct(Typing::author))
        .onBackpressureLatest());
  }

  @Override
  public void archive(String roomId) { archived.emitNext(roomId, RETRY); }

  @Override
  public int watching(String roomId) { return watching.getOrDefault(roomId, new AtomicInteger()).get(); }

  /**
   * Every subscription on a room: completes when the room is archived (takeUntilOther), and is counted
   * while open. doFinally runs on complete, error and cancel; a client that just disconnects cancels.
   */
  private <T> Flux<T> watched(String roomId, Flux<T> events) {
    AtomicInteger n = watching.computeIfAbsent(roomId, k -> new AtomicInteger());
    return events.takeUntilOther(archived.asFlux().filter(roomId::equals))
        .doOnSubscribe(s -> n.incrementAndGet())
        .doFinally(s -> n.decrementAndGet());
  }
}

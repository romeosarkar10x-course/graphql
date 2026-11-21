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
  // "Never lose a typing event": buffer them.
  private final Sinks.Many<Typing> typing = Sinks.many().multicast().onBackpressureBuffer(1_000_000, false);
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

  // TODO lab 11.3: the subscriber sees its own typing, and every one of 10,000 events a second.
  @Override
  public Flux<Typing> typing(String roomId, String me) {
    return watched(roomId, typing.asFlux().filter(t -> t.roomId().equals(roomId)));
  }

  @Override
  public void archive(String roomId) { archived.emitNext(roomId, RETRY); }

  @Override
  public int watching(String roomId) { return watching.getOrDefault(roomId, new AtomicInteger()).get(); }

  // TODO lab 11.3: archiving a room should complete its subscriptions, and the count looks wrong after
  // clients go away.
  private <T> Flux<T> watched(String roomId, Flux<T> events) {
    AtomicInteger n = watching.computeIfAbsent(roomId, k -> new AtomicInteger());
    return events.doOnSubscribe(s -> n.incrementAndGet()).doOnComplete(n::decrementAndGet);
  }
}

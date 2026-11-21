package dev.shelfie.live;

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

  @Override
  public void publish(Message m) { messages.emitNext(m, Sinks.EmitFailureHandler.busyLooping(java.time.Duration.ofMillis(50))); }

  @Override
  public Flux<Message> messages(String roomId) {
    return watched(roomId, messages.asFlux().filter(m -> m.roomId().equals(roomId)));
  }

  @Override
  public void typing(Typing t) { typing.emitNext(t, Sinks.EmitFailureHandler.busyLooping(java.time.Duration.ofMillis(50))); }

  @Override
  public Flux<Typing> typing(String roomId, String me) {
    return watched(roomId, typing.asFlux().filter(t -> t.roomId().equals(roomId)));
  }

  @Override
  public void archive(String roomId) { archived.emitNext(roomId, Sinks.EmitFailureHandler.busyLooping(java.time.Duration.ofMillis(50))); }

  @Override
  public int watching(String roomId) { return watching.getOrDefault(roomId, new AtomicInteger()).get(); }

  private <T> Flux<T> watched(String roomId, Flux<T> events) {
    AtomicInteger n = watching.computeIfAbsent(roomId, k -> new AtomicInteger());
    return events.doOnSubscribe(s -> n.incrementAndGet()).doFinally(s -> n.decrementAndGet());
  }
}

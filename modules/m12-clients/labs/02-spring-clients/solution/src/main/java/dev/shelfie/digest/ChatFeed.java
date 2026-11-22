package dev.shelfie.digest;

import dev.shelfie.digest.Model.Message;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.graphql.client.WebSocketGraphQlClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.Disposable;
import reactor.util.retry.Retry;

/**
 * Subscribes to one chat room for the life of the service and keeps the last 20 messages. When the chat
 * server goes away the subscription errors; retryWhen subscribes again (a new WebSocket session), forever,
 * with backoff capped at 2 s.
 */
@RestController
public class ChatFeed {
  private static final Logger log = LoggerFactory.getLogger(ChatFeed.class);
  private final WebSocketGraphQlClient chat;
  private final String room;
  private final Deque<Message> recent = new ArrayDeque<>();
  private Disposable subscription;

  ChatFeed(WebSocketGraphQlClient chat, @Value("${digest.room}") String room) {
    this.chat = chat;
    this.room = room;
  }

  @EventListener(ApplicationReadyEvent.class)
  void start() {
    subscription = chat.document("subscription feed($room: ID!) { messageSent(roomId: $room) { id author body } }")
        .variable("room", room)
        .retrieveSubscription("messageSent").toEntity(Message.class)
        .doOnSubscribe(s -> log.info("subscribed to {}", room))
        .doOnError(e -> log.info("chat subscription ended: {}", e.toString()))
        .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofMillis(200)).maxBackoff(Duration.ofSeconds(2)))
        .subscribe(this::add);
  }

  private synchronized void add(Message m) {
    recent.addLast(m);
    while (recent.size() > 20) recent.removeFirst();
  }

  @GetMapping("/chat")
  synchronized List<Message> recent() {
    return List.copyOf(recent);
  }

  @PreDestroy
  void stop() {
    if (subscription != null) subscription.dispose();
  }
}

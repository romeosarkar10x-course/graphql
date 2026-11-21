package dev.shelfie.live;

import reactor.core.publisher.Flux;

/** Where events travel from a mutation to every subscriber. Lab 11.4 makes it span server instances. */
public interface Bus {
  void publish(Message m);

  Flux<Message> messages(String roomId);

  void typing(Typing t);

  Flux<Typing> typing(String roomId, String me);

  void archive(String roomId);

  /** Open subscriptions on a room. */
  int watching(String roomId);
}

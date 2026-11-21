package dev.shelfie.live;

/** Carries events to every instance (lab 11.4). Each implementation hands what it receives to LocalBus.deliver. */
public interface Relay {
  void send(Event e);
}

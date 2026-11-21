package dev.shelfie.live;

/** What travels between instances: one of a message, a typing event or an archived room. */
public record Event(String kind, String roomId, Message message, Typing typing) {}

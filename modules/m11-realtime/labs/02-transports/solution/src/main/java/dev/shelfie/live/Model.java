package dev.shelfie.live;

import java.util.List;

record Room(String id, String name) {}

record Message(String id, String roomId, String author, String body, String sentAt) {
  long seq() { return Long.parseLong(id.substring(1)); }
}

record Typing(String roomId, String author) {}

record Club(String id, String name, String description, List<String> memberIds) {}

record Reader(String id, String name) {}

record Book(String id, String title) {}

record Review(String id, String clubId, String bookId, int rating, String body) {}

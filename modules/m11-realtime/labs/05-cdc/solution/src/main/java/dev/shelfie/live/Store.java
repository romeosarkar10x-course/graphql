package dev.shelfie.live;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Module 11's seed: 3 rooms, 6 readers, 3 clubs, 12 books, 9 reviews, 2 messages per room. Lab 11.5:
 * messages live in PostgreSQL (table m11.messages, recreated at startup); everything else in memory.
 */
@Component
public class Store {
  final Map<String, Room> rooms = new LinkedHashMap<>();
  final Set<String> archived = ConcurrentHashMap.newKeySet();
  private final JdbcClient db;
  final Map<String, Club> clubs = new LinkedHashMap<>();
  final Map<String, Reader> readers = new LinkedHashMap<>();
  final Map<String, Book> books = new LinkedHashMap<>();
  final List<Review> reviews = new ArrayList<>();
  public Store(JdbcClient db) {
    this.db = db;
    db.sql("CREATE SCHEMA IF NOT EXISTS m11").update();
    db.sql("DROP TABLE IF EXISTS m11.messages").update();
    db.sql("""
        CREATE TABLE m11.messages (
          id bigserial PRIMARY KEY,
          room_id text NOT NULL,
          author text NOT NULL,
          body text NOT NULL,
          sent_at timestamptz NOT NULL DEFAULT now())""").update();
    for (String[] r : new String[][] {{"r1", "general"}, {"r2", "sci-fi"}, {"r3", "mystery"}}) rooms.put(r[0], new Room(r[0], r[1]));
    String[] names = {"Ada", "Ben", "Cleo", "Dev", "Esi", "Femi"};
    for (int i = 0; i < names.length; i++) readers.put("u" + (i + 1), new Reader("u" + (i + 1), names[i]));
    String[] titles = {"Dune", "Kindred", "The Dispossessed", "Rebecca", "The Secret History", "Gone Girl",
        "Piranesi", "Station Eleven", "The Big Sleep", "Neuromancer", "Beloved", "The Name of the Rose"};
    for (int i = 0; i < titles.length; i++) books.put("b" + (i + 1), new Book("b" + (i + 1), titles[i]));
    clubs.put("c1", new Club("c1", "Night Owls", "Science fiction after dark.", List.of("u1", "u2", "u3")));
    clubs.put("c2", new Club("c2", "Paper Trail", "Mysteries and the people who solve them.", List.of("u3", "u4", "u5")));
    clubs.put("c3", new Club("c3", "Slow Readers", "One chapter a week, no spoilers.", List.of("u1", "u5", "u6")));
    int n = 0;
    for (String c : clubs.keySet())
      for (int k = 0; k < 3; k++) {
        n++;
        reviews.add(new Review("v" + n, c, "b" + ((n * 5) % 12 + 1), 3 + n % 3, "Review " + n + " of " + clubs.get(c).name()));
      }
    for (String room : rooms.keySet()) {
      add(room, "Ada", "Welcome to #" + rooms.get(room).name());
      add(room, "Ben", "Hi all");
    }
  }

  Message add(String roomId, String author, String body) {
    return db.sql("INSERT INTO m11.messages (room_id, author, body) VALUES (?, ?, ?) RETURNING id, room_id, author, body, sent_at")
        .params(roomId, author, body).query(Store::row).single();
  }

  List<Message> in(String roomId) {
    return db.sql("SELECT id, room_id, author, body, sent_at FROM m11.messages WHERE room_id = ? ORDER BY id").param(roomId).query(Store::row).list();
  }

  static Message row(java.sql.ResultSet r, int n) throws java.sql.SQLException {
    return new Message("m" + r.getLong("id"), r.getString("room_id"), r.getString("author"), r.getString("body"),
        r.getObject("sent_at", java.time.OffsetDateTime.class).toInstant().toString());
  }
}

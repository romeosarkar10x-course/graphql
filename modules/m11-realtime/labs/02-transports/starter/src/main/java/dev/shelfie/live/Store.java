package dev.shelfie.live;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/** Module 11's seed: 3 rooms, 6 readers, 3 clubs, 12 books, 9 reviews, 2 messages per room. In memory. */
@Component
public class Store {
  final Map<String, Room> rooms = new LinkedHashMap<>();
  final Set<String> archived = ConcurrentHashMap.newKeySet();
  final List<Message> messages = new CopyOnWriteArrayList<>();
  final Map<String, Club> clubs = new LinkedHashMap<>();
  final Map<String, Reader> readers = new LinkedHashMap<>();
  final Map<String, Book> books = new LinkedHashMap<>();
  final List<Review> reviews = new ArrayList<>();
  private final AtomicLong seq = new AtomicLong();

  public Store() {
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
    Message m = new Message("m" + seq.incrementAndGet(), roomId, author, body, Instant.now().toString());
    messages.add(m);
    return m;
  }

  List<Message> in(String roomId) { return messages.stream().filter(m -> m.roomId().equals(roomId)).toList(); }
}

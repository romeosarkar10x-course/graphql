package dev.shelfie.secure;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Module 10's dataset, in memory. Every row is a function of its index, so responses are exact.
 * 40 readers, 12 clubs (c1 and c3 private), 60 books, about 300 reviews. The five Keycloak users
 * (contract/keycloak/shelfie-realm.json) are r1 ada, r2 ben, r3 max (moderator), r4 olu, r5 root (admin).
 * Do not change it: the lab tests depend on it.
 */
@Component
public class Store {
  public final Map<String, Reader> readers = new LinkedHashMap<>();
  public final Map<String, Club> clubs = new ConcurrentHashMap<>();
  public final Map<String, Book> books = new LinkedHashMap<>();
  public final Map<String, Review> reviews = new ConcurrentHashMap<>();
  private final AtomicInteger nextReview = new AtomicInteger();
  private final Sinks.Many<Review> added = Sinks.many().multicast().directBestEffort();

  public Store() {
    String[] named = {"ada", "ben", "max", "olu", "root"};
    for (int i = 1; i <= 40; i++) {
      String user = i <= 5 ? named[i - 1] : "reader" + i;
      String name = i <= 5 ? Character.toUpperCase(user.charAt(0)) + user.substring(1) : "Reader " + i;
      List<String> roles = switch (i) {
        case 3 -> List.of("member", "moderator");
        case 5 -> List.of("member", "admin");
        default -> List.of("member");
      };
      readers.put("r" + i, new Reader("r" + i, user, name, user + "@shelfie.test", roles));
    }
    addClub(1, "Night Owls", true, 4, List.of(4, 1, 3, 6, 7, 8, 9, 10, 11, 12), "4821");
    addClub(2, "Open Shelf", false, 1, List.of(1, 2, 13, 14, 15, 16, 17, 18, 19, 20), "1111");
    addClub(3, "Quiet Corner", true, 2, List.of(2, 21, 22, 23, 24, 25, 26, 27, 28), "0937");
    String[] clubNames = {"Sci-Fi Sundays", "Poetry Pit", "Classics Club", "Graphic Novels", "Crime Scene",
        "History Buffs", "Fantasy Forge", "Short Stories", "Translated Fiction"};
    for (int k = 4; k <= 12; k++) {
      LinkedHashSet<Integer> m = new LinkedHashSet<>(List.of(k + 2));
      for (int j = 0; m.size() < 8; j++) m.add((k * 5 + j * 4) % 35 + 6);
      addClub(k, clubNames[k - 4], false, k + 2, List.copyOf(m), String.format("%04d", (k * 7919) % 10000));
    }
    String[] adj = {"Silent", "Hidden", "Last", "Burning", "Paper", "Glass", "Winter", "Lost", "Iron", "Quiet"};
    String[] noun = {"Garden", "River", "Library", "Engine", "Lantern", "Harbour"};
    for (int i = 1; i <= 60; i++) books.put("b" + i, new Book("b" + i, "The " + adj[(i - 1) % 10] + " " + noun[(i - 1) / 10]));
    for (int c = 1; c <= 12; c++) {
      Club club = clubs.get("c" + c);
      for (int m = 0; m < club.memberIds().size(); m++) {
        Reader author = readers.get(club.memberIds().get(m));
        for (int n = 0; n < 3; n++) {
          String book = "b" + ((c * 13 + m * 7 + n * 11) % 60 + 1);
          String body = club.isPrivate()
              ? author.name() + "'s members-only note on " + books.get(book).title() + " for " + club.name()
              : author.name() + " on " + books.get(book).title();
          add(new Review(nextId(), 1 + (c + m + n) % 5, body, author.id(), book, club.id()));
        }
      }
    }
  }

  private void addClub(int n, String name, boolean priv, int owner, List<Integer> members, String code) {
    clubs.put("c" + n, new Club("c" + n, name, priv, "r" + owner, members.stream().map(i -> "r" + i).toList(), code));
  }

  private String nextId() { return "v" + nextReview.incrementAndGet(); }

  private void add(Review r) { reviews.put(r.id(), r); }

  public Reader byUsername(String username) {
    return readers.values().stream().filter(r -> r.username().equals(username)).findFirst().orElse(null);
  }

  public List<Club> clubsInOrder() {
    return clubs.values().stream().sorted(Comparator.comparingInt(c -> Integer.parseInt(c.id().substring(1)))).toList();
  }

  public List<Review> reviewsWhere(java.util.function.Predicate<Review> p) {
    return reviews.values().stream().filter(p)
        .sorted(Comparator.comparingInt(r -> Integer.parseInt(r.id().substring(1)))).toList();
  }

  public Review addReview(String clubId, String bookId, int rating, String body, String authorId) {
    Review r = new Review(nextId(), rating, body, authorId, bookId, clubId);
    add(r);
    added.tryEmitNext(r);
    return r;
  }

  public Flux<Review> reviewsAdded() { return added.asFlux(); }

  public Club rename(String id, String name) {
    return clubs.computeIfPresent(id, (k, c) -> new Club(c.id(), name, c.isPrivate(), c.ownerId(), c.memberIds(), c.inviteCode()));
  }

  public Club join(String id, String readerId) {
    return clubs.computeIfPresent(id, (k, c) -> {
      if (c.memberIds().contains(readerId)) return c;
      List<String> m = new ArrayList<>(c.memberIds());
      m.add(readerId);
      return new Club(c.id(), c.name(), c.isPrivate(), c.ownerId(), List.copyOf(m), c.inviteCode());
    });
  }
}

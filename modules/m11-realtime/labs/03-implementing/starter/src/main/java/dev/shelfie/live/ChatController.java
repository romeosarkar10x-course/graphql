package dev.shelfie.live;

import java.time.Duration;
import java.util.List;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Controller
public class ChatController {
  private final Store store;
  private final Bus bus;

  public ChatController(Store store, Bus bus) {
    this.store = store;
    this.bus = bus;
  }

  @QueryMapping
  public List<Room> rooms() { return List.copyOf(store.rooms.values()); }

  @QueryMapping
  public Room room(@Argument String id) { return store.rooms.get(id); }

  @QueryMapping
  public List<Message> messages(@Argument String roomId, @Argument String after, @Argument int last) {
    long from = after == null ? 0 : Long.parseLong(after.substring(1));
    List<Message> in = store.in(roomId).stream().filter(m -> m.seq() > from).toList();
    return in.subList(Math.max(0, in.size() - last), in.size());
  }

  @SchemaMapping
  public List<Message> messages(Room room, @Argument int last) {
    List<Message> in = store.in(room.id());
    return in.subList(Math.max(0, in.size() - last), in.size());
  }

  @SchemaMapping
  public int watching(Room room) { return bus.watching(room.id()); }

  @SchemaMapping
  public boolean archived(Room room) { return store.archived.contains(room.id()); }

  @MutationMapping
  public Message sendMessage(@Argument String roomId, @Argument String author, @Argument String body) {
    if (!store.rooms.containsKey(roomId)) throw new Problem(ErrorType.NOT_FOUND, "no room " + roomId);
    if (store.archived.contains(roomId)) throw new Problem(ErrorType.BAD_REQUEST, "room " + roomId + " is archived");
    Message m = store.add(roomId, author, body);
    bus.publish(m);
    return m;
  }

  @MutationMapping
  public boolean typing(@Argument String roomId, @Argument String author) {
    bus.typing(new Typing(roomId, author));
    return true;
  }

  @MutationMapping
  public Room archiveRoom(@Argument String id) {
    Room r = store.rooms.get(id);
    if (r == null) throw new Problem(ErrorType.NOT_FOUND, "no room " + id);
    store.archived.add(id);
    bus.archive(id);
    return r;
  }

  // Lecture 11.1: one mutation publishes once; the subscription's source stream is the bus, filtered to
  // one room; graphql-java maps each source event through the selection set (the response stream).
  @SubscriptionMapping
  public Flux<Message> messageSent(@Argument String roomId) {
    if (!store.rooms.containsKey(roomId)) return Flux.error(new Problem(ErrorType.NOT_FOUND, "no room " + roomId));
    return bus.messages(roomId);
  }

  @SubscriptionMapping("typing")
  public Flux<Typing> typingIn(@Argument String roomId, @Argument String me) {
    return bus.typing(roomId, me);
  }

  @SubscriptionMapping
  public Flux<Integer> countdown(@Argument int from, @Argument Integer failAt) {
    return Flux.interval(Duration.ofMillis(100)).take(Math.max(from, 0)).map(i -> {
      int n = from - i.intValue();
      if (failAt != null && n == failAt) throw new SourceStreamFailed("countdown source failed at " + n + " (failAt: " + failAt + ")");
      return n;
    });
  }

  @SchemaMapping
  public List<Reader> members(Club club) { return club.memberIds().stream().map(store.readers::get).toList(); }

  @QueryMapping
  public Club club(@Argument String id) { return store.clubs.get(id); }

  @SchemaMapping
  public List<Review> reviews(Club club, @Argument int first) {
    return store.reviews.stream().filter(r -> r.clubId().equals(club.id())).limit(first).toList();
  }

  @SchemaMapping
  public Book book(Review review) { return store.books.get(review.bookId()); }

  @SchemaMapping
  public List<Book> recommendations(Club club) throws InterruptedException {
    Thread.sleep(1500); // the recommender is slow
    int n = Integer.parseInt(club.id().substring(1));
    return List.of(store.books.get("b" + n), store.books.get("b" + (n + 3)), store.books.get("b" + (n + 6)));
  }
}

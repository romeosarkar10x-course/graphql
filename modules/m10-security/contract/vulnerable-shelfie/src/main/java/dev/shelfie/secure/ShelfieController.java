package dev.shelfie.secure;

import java.util.List;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

@Controller
public class ShelfieController {
  private final Store store;
  private final Viewer viewer;

  public ShelfieController(Store store, Viewer viewer) {
    this.store = store;
    this.viewer = viewer;
  }

  @QueryMapping
  public Reader viewer(@AuthenticationPrincipal Jwt jwt) {
    return jwt == null ? null : store.byUsername(jwt.getClaimAsString("preferred_username"));
  }

  @QueryMapping
  public Object node(@Argument String id) {
    if (id.isEmpty()) return null;
    return switch (id.charAt(0)) {
      case 'r' -> store.readers.get(id);
      case 'c' -> store.clubs.get(id);
      case 'b' -> store.books.get(id);
      case 'v' -> store.reviews.get(id);
      default -> null;
    };
  }

  @QueryMapping
  public Club club(@Argument String id) { return visible(store.clubs.get(id)); }

  // "Private clubs are hidden from club and clubs."
  private Club visible(Club c) {
    Reader me = viewer.current();
    return c == null || !c.isPrivate() || (me != null && c.memberIds().contains(me.id())) ? c : null;
  }

  @QueryMapping
  public List<Club> clubs(@Argument int first) {
    return store.clubsInOrder().stream().filter(c -> visible(c) != null).limit(first).toList();
  }

  @QueryMapping
  public Book book(@Argument String id) { return store.books.get(id); }

  @QueryMapping
  public List<Book> books(@Argument int first) { return store.books.values().stream().limit(first).toList(); }

  @QueryMapping
  public List<Reader> readers(@Argument int first) {
    return store.readers.values().stream().filter(r -> !r.username().equals("ghost")).limit(first).toList();
  }

  @SchemaMapping
  public List<String> roles(Reader reader) { return reader.roles().stream().map(String::toUpperCase).toList(); }

  @SchemaMapping
  public List<Club> clubs(Reader reader, @Argument int first) {
    return store.clubsInOrder().stream().filter(c -> c.memberIds().contains(reader.id())).limit(first).toList();
  }

  @SchemaMapping(field = "private")
  public boolean isPrivate(Club club) { return club.isPrivate(); }

  @SchemaMapping
  public Reader owner(Club club) { return store.readers.get(club.ownerId()); }

  @SchemaMapping
  public List<Reader> members(Club club, @Argument int first) {
    return club.memberIds().stream().limit(first).map(store.readers::get).toList();
  }

  @SchemaMapping
  public List<Review> reviews(Club club, @Argument int first) {
    return store.reviewsWhere(r -> r.clubId().equals(club.id())).stream().limit(first).toList();
  }

  @SchemaMapping
  public List<Review> reviews(Book book, @Argument int first) {
    return store.reviewsWhere(r -> r.bookId().equals(book.id())).stream().limit(first).toList();
  }

  @SchemaMapping
  public Reader author(Review review) { return store.readers.get(review.authorId()); }

  @SchemaMapping
  public Book book(Review review) { return store.books.get(review.bookId()); }

  @SchemaMapping
  public Club club(Review review) { return store.clubs.get(review.clubId()); }

  @SchemaMapping
  public List<Book> recommendations(Book book, @Argument int first) throws InterruptedException {
    Thread.sleep(300); // the recommender is slow
    int n = Integer.parseInt(book.id().substring(1));
    return java.util.stream.IntStream.range(0, first).mapToObj(i -> store.books.get("b" + ((n + i) % 60 + 1))).toList();
  }

  @MutationMapping
  public Review addReview(@Argument String clubId, @Argument String bookId, @Argument int rating, @Argument String body) {
    Reader me = viewer.require();
    if (!store.clubs.containsKey(clubId) || !store.books.containsKey(bookId)) throw new Denied(ErrorType.BAD_REQUEST, "no such club or book");
    return store.addReview(clubId, bookId, rating, body, me.id());
  }

  @MutationMapping
  public String removeReview(@Argument String id) {
    viewer.require();
    if (store.reviews.remove(id) == null) throw new Denied(ErrorType.NOT_FOUND, "no review " + id);
    return id;
  }

  @MutationMapping
  public Club renameClub(@Argument String id, @Argument String name) {
    viewer.require();
    Club c = store.rename(id, name);
    if (c == null) throw new Denied(ErrorType.NOT_FOUND, "no club " + id);
    return c;
  }

  @MutationMapping
  public Club joinClub(@Argument String id, @Argument String inviteCode, graphql.schema.DataFetchingEnvironment env) {
    Reader me = viewer.require();
    if (!Boolean.TRUE.equals(env.getGraphQlContext().get("joinAllowed"))) throw new Denied(ErrorType.BAD_REQUEST, "rate limited: 10 joinClub requests a minute");
    Club c = store.clubs.get(id);
    return c != null && c.inviteCode().equals(inviteCode) ? store.join(id, me.id()) : null;
  }

  @SubscriptionMapping
  public Flux<Review> reviewAdded(@Argument String clubId) {
    return store.reviewsAdded().filter(r -> r.clubId().equals(clubId));
  }
}

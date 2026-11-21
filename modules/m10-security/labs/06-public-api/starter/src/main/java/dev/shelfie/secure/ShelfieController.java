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
  private final Access access;
  private final ClubService clubs;

  public ShelfieController(Store store, Viewer viewer, Access access, ClubService clubs) {
    this.store = store;
    this.viewer = viewer;
    this.access = access;
    this.clubs = clubs;
  }

  // Lecture 10.1: the token was checked by the HTTP layer; the resolver only reads who it belongs to.
  @QueryMapping
  public Reader viewer(@AuthenticationPrincipal Jwt jwt) {
    return jwt == null ? null : store.byUsername(jwt.getClaimAsString("preferred_username"));
  }

  @QueryMapping
  public Object node(@Argument String id) {
    if (id.isEmpty()) return null;
    return switch (id.charAt(0)) {
      case 'r' -> store.readers.get(id);
      // node(id:) is a second door to every object: it must ask the same rules as club(id:).
      case 'c' -> visible(store.clubs.get(id));
      case 'b' -> store.books.get(id);
      case 'v' -> visible(store.reviews.get(id));
      default -> null;
    };
  }

  @QueryMapping
  public Club club(@Argument String id) { return visible(store.clubs.get(id)); }

  private Club visible(Club c) { return access.canSee(c) ? c : null; }

  private Review visible(Review r) { return access.canSee(r) ? r : null; }

  @QueryMapping
  public List<Club> clubs(@Argument int first) { return store.clubsInOrder().stream().filter(access::canSee).limit(first).toList(); }

  @QueryMapping
  public Book book(@Argument String id) { return store.books.get(id); }

  @QueryMapping
  public List<Book> books(@Argument int first) { return store.books.values().stream().limit(first).toList(); }

  @QueryMapping
  public List<Reader> readers(@Argument int first) { return store.readers.values().stream().limit(first).toList(); }

  @SchemaMapping
  public List<String> roles(Reader reader) { return reader.roles().stream().map(String::toUpperCase).toList(); }

  @SchemaMapping
  public List<Club> clubs(Reader reader, @Argument int first) {
    return store.clubsInOrder().stream().filter(c -> c.memberIds().contains(reader.id()))
        .filter(access::canSee).limit(first).toList();
  }

  @SchemaMapping
  public String email(Reader reader) { return access.canSeeEmail(reader) ? reader.email() : null; }

  @SchemaMapping
  public String inviteCode(Club club) { return access.canSeeInviteCode(club) ? club.inviteCode() : null; }

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
    // A list is a third door: filter it by the same rule, or private reviews leak through books.
    return store.reviewsWhere(r -> r.bookId().equals(book.id()) && access.canSee(r)).stream().limit(first).toList();
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
    return clubs.addReview(clubId, bookId, rating, body, viewer.require());
  }

  @MutationMapping
  public String removeReview(@Argument String id) {
    viewer.require();
    if (!store.reviews.containsKey(id)) throw new Denied(ErrorType.NOT_FOUND, "no review " + id);
    return clubs.removeReview(id);
  }

  @MutationMapping
  public Club renameClub(@Argument String id, @Argument String name) {
    viewer.require();
    if (!store.clubs.containsKey(id)) throw new Denied(ErrorType.NOT_FOUND, "no club " + id);
    return clubs.rename(id, name);
  }

  @MutationMapping
  public Club joinClub(@Argument String id, @Argument String inviteCode) {
    Reader me = viewer.require();
    Club c = store.clubs.get(id);
    return c != null && c.inviteCode().equals(inviteCode) ? store.join(id, me.id()) : null;
  }

  @SubscriptionMapping
  public Flux<Review> reviewAdded(@Argument String clubId) {
    // Decide at subscribe time, on the subscriber's own request: the filter below runs later, on the
    // thread of whoever adds a review, where the security context is theirs, not the subscriber's.
    if (!access.canSee(store.clubs.get(clubId))) throw new Denied(ErrorType.FORBIDDEN, "not a member of " + clubId);
    return store.reviewsAdded().filter(r -> r.clubId().equals(clubId));
  }
}

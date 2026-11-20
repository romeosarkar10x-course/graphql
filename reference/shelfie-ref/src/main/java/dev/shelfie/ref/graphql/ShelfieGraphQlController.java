package dev.shelfie.ref.graphql;

import dev.shelfie.ref.data.Model.Book;
import dev.shelfie.ref.data.Model.Club;
import dev.shelfie.ref.data.Model.Meetup;
import dev.shelfie.ref.data.Model.Review;
import dev.shelfie.ref.data.Model.User;
import dev.shelfie.ref.data.Store;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

/**
 * GraphQL over the same Store. You do not need to understand this yet: Module 5 builds it by hand
 * and Module 6 explains every annotation. For now it is "a ready-made GraphQL endpoint" (1.3).
 */
@Controller
public class ShelfieGraphQlController {

    private final Store store;

    public ShelfieGraphQlController(Store store) {
        this.store = store;
    }

    @QueryMapping
    public String hello() {
        return "Hello from Shelfie!";
    }

    @QueryMapping
    public Club club(@Argument String id) {
        return store.club(id).orElse(null);
    }

    @QueryMapping
    public List<Club> clubs() {
        return store.clubs();
    }

    @QueryMapping
    public User user(@Argument String id) {
        return store.user(id).orElse(null);
    }

    @QueryMapping
    public Book book(@Argument String id) {
        return store.book(id).orElse(null);
    }

    @QueryMapping
    public List<Book> books(@Argument String titleContains, @Argument int first) {
        String needle = titleContains == null ? "" : titleContains.toLowerCase(Locale.ROOT);
        return store.books().stream()
                .filter(b -> b.title().toLowerCase(Locale.ROOT).contains(needle))
                .limit(Math.max(0, first))
                .toList();
    }

    /** Union members resolve by class name: the records are called Book, Club and User. */
    @QueryMapping
    public List<Object> search(@Argument String text) {
        String needle = text.toLowerCase(Locale.ROOT);
        List<Object> hits = new ArrayList<>();
        store.books().stream().filter(b -> b.title().toLowerCase(Locale.ROOT).contains(needle)).forEach(hits::add);
        store.clubs().stream().filter(c -> c.name().toLowerCase(Locale.ROOT).contains(needle)).forEach(hits::add);
        store.users().stream()
                .filter(u -> u.name().toLowerCase(Locale.ROOT).contains(needle) || u.handle().toLowerCase(Locale.ROOT).contains(needle))
                .forEach(hits::add);
        return hits;
    }

    /** Never emits: the schema needs a Subscription type for Module 2.5; delivery arrives in Module 11. */
    @SubscriptionMapping
    public Flux<Meetup> meetupScheduled(@Argument String clubId) {
        return Flux.never();
    }

    @SchemaMapping
    public User owner(Club club) {
        return store.user(club.ownerId()).orElseThrow();
    }

    @SchemaMapping
    public List<User> members(Club club, @Argument int first) {
        return club.memberIds().stream().limit(Math.max(0, first)).map(id -> store.user(id).orElseThrow()).toList();
    }

    @SchemaMapping
    public int memberCount(Club club) {
        return club.memberIds().size();
    }

    @SchemaMapping
    public Book currentBook(Club club) {
        return club.currentBookId() == null ? null : store.book(club.currentBookId()).orElse(null);
    }

    @SchemaMapping
    public List<Meetup> meetups(Club club, @Argument boolean upcomingOnly) {
        return store.meetupsForClub(club.id(), upcomingOnly);
    }

    @SchemaMapping(typeName = "User")
    public List<Club> clubs(User user) {
        return store.clubsOfUser(user.id());
    }

    @SchemaMapping(typeName = "User")
    public List<Review> reviews(User user) {
        return store.reviewsByUser(user.id());
    }

    @SchemaMapping
    public Double averageRating(Book book) {
        return store.averageRating(book.id());
    }

    @SchemaMapping(typeName = "Book")
    public List<Review> reviews(Book book, @Argument int last) {
        return store.reviewsForBook(book.id()).stream().limit(Math.max(0, last)).toList();
    }

    @SchemaMapping
    public User author(Review review) {
        return store.user(review.authorId()).orElseThrow();
    }

    @SchemaMapping
    public Book book(Review review) {
        return store.book(review.bookId()).orElseThrow();
    }

    @SchemaMapping
    public Club club(Meetup meetup) {
        return store.club(meetup.clubId()).orElseThrow();
    }
}

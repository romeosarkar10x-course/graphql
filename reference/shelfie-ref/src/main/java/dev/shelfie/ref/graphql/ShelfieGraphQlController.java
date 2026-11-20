package dev.shelfie.ref.graphql;

import dev.shelfie.ref.data.Model.Book;
import dev.shelfie.ref.data.Model.Club;
import dev.shelfie.ref.data.Model.Meetup;
import dev.shelfie.ref.data.Model.Review;
import dev.shelfie.ref.data.Model.User;
import dev.shelfie.ref.data.Store;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

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
    public List<Book> books() {
        return store.books();
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

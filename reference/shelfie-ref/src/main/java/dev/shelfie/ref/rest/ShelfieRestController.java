package dev.shelfie.ref.rest;

import dev.shelfie.ref.data.Model.Book;
import dev.shelfie.ref.data.Model.Club;
import dev.shelfie.ref.data.Model.Meetup;
import dev.shelfie.ref.data.Model.Review;
import dev.shelfie.ref.data.Model.User;
import dev.shelfie.ref.data.Store;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * A deliberately ordinary resource-oriented REST API. Resources reference each other by ID, so a
 * screen that needs a club, its members, its book and that book's reviewers must make a waterfall
 * of calls (Module 1.2). Every resource returns every field (over-fetching).
 */
@RestController
@RequestMapping("/api")
public class ShelfieRestController {

    private final Store store;

    public ShelfieRestController(Store store) {
        this.store = store;
    }

    @GetMapping("/clubs")
    public List<Club> clubs() {
        return store.clubs();
    }

    @GetMapping("/clubs/{id}")
    public Club club(@PathVariable String id) {
        return store.club(id).orElseThrow(() -> notFound("club", id));
    }

    @GetMapping("/clubs/{id}/meetups")
    public List<Meetup> meetups(@PathVariable String id) {
        club(id);
        return store.meetupsForClub(id, false);
    }

    @GetMapping("/users/{id}")
    public User user(@PathVariable String id) {
        return store.user(id).orElseThrow(() -> notFound("user", id));
    }

    @GetMapping("/books/{id}")
    public Book book(@PathVariable String id) {
        return store.book(id).orElseThrow(() -> notFound("book", id));
    }

    @GetMapping("/books/{id}/reviews")
    public List<Review> reviews(@PathVariable String id) {
        book(id);
        return store.reviewsForBook(id);
    }

    private static ResponseStatusException notFound(String kind, String id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, kind + " " + id + " not found");
    }
}

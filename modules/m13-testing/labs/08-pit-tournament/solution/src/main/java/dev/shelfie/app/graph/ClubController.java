package dev.shelfie.app.graph;

import dev.shelfie.app.service.CatalogService;
import dev.shelfie.app.service.ReviewService;
import dev.shelfie.app.store.Records.Book;
import dev.shelfie.app.store.Records.Club;
import dev.shelfie.app.store.Records.Meetup;
import dev.shelfie.app.store.Records.Review;
import dev.shelfie.app.store.Records.User;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/** Club, User and Meetup fields that are not plain record components. */
@Controller
public class ClubController {

    private final CatalogService catalog;
    private final ReviewService reviews;

    public ClubController(CatalogService catalog, ReviewService reviews) {
        this.catalog = catalog;
        this.reviews = reviews;
    }

    @SchemaMapping public User owner(Club club) { return catalog.owner(club); }
    @SchemaMapping public List<User> members(Club club, @Argument int first) { return catalog.members(club, first); }
    @SchemaMapping public int memberCount(Club club) { return catalog.memberCount(club); }
    @SchemaMapping public Book currentBook(Club club) { return catalog.currentBook(club).orElse(null); }
    @SchemaMapping public List<Meetup> meetups(Club club, @Argument boolean upcomingOnly) { return catalog.meetups(club, upcomingOnly); }

    @SchemaMapping(typeName = "User") public List<Club> clubs(User user) { return catalog.clubsOf(user); }
    @SchemaMapping(typeName = "User") public List<Review> reviews(User user) { return reviews.reviewsBy(user.id()); }

    @SchemaMapping(typeName = "Meetup") public Club club(Meetup meetup) { return catalog.club(meetup.clubId()).orElseThrow(); }
}

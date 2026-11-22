package dev.shelfie.app.graph;

import dev.shelfie.app.generated.DgsConstants.CLUB;
import dev.shelfie.app.generated.DgsConstants.MEETUP;
import dev.shelfie.app.generated.DgsConstants.USER;
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

/** Club, User and Meetup fields that are not plain record components, named by DgsConstants. */
@Controller
public class ClubController {

    private final CatalogService catalog;
    private final ReviewService reviews;

    public ClubController(CatalogService catalog, ReviewService reviews) {
        this.catalog = catalog;
        this.reviews = reviews;
    }

    @SchemaMapping(typeName = CLUB.TYPE_NAME, field = CLUB.Owner) public User owner(Club club) { return catalog.owner(club); }

    @SchemaMapping(typeName = CLUB.TYPE_NAME, field = CLUB.Members)
    public List<User> members(Club club, @Argument(CLUB.MEMBERS_INPUT_ARGUMENT.First) int first) { return catalog.members(club, first); }

    @SchemaMapping(typeName = CLUB.TYPE_NAME, field = CLUB.MemberCount) public int memberCount(Club club) { return catalog.memberCount(club); }
    @SchemaMapping(typeName = CLUB.TYPE_NAME, field = CLUB.CurrentBook) public Book currentBook(Club club) { return catalog.currentBook(club).orElse(null); }

    @SchemaMapping(typeName = CLUB.TYPE_NAME, field = CLUB.Meetups)
    public List<Meetup> meetups(Club club, @Argument(CLUB.MEETUPS_INPUT_ARGUMENT.UpcomingOnly) boolean upcomingOnly) {
        return catalog.meetups(club, upcomingOnly);
    }

    @SchemaMapping(typeName = USER.TYPE_NAME, field = USER.Clubs) public List<Club> clubs(User user) { return catalog.clubsOf(user); }
    @SchemaMapping(typeName = USER.TYPE_NAME, field = USER.Reviews) public List<Review> reviews(User user) { return reviews.reviewsBy(user.id()); }

    @SchemaMapping(typeName = MEETUP.TYPE_NAME, field = MEETUP.Club) public Club club(Meetup meetup) { return catalog.club(meetup.clubId()).orElseThrow(); }
}

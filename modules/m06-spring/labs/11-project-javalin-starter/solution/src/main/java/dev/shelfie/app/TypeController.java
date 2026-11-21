package dev.shelfie.app;

import dev.shelfie.tiny.Arg;
import dev.shelfie.tiny.Field;
import dev.shelfie.tiny.GraphQLController;
import java.util.List;

/** Fields of the object types: the first parameter is the parent. */
@GraphQLController
public class TypeController {
    @Field(type = "Club") public Data.User owner(Data.Club c) { return Data.user(c.ownerId()).orElseThrow(); }

    @Field(type = "Club")
    public List<Data.User> members(Data.Club c, @Arg Integer first) {
        return c.memberIds().stream().limit(first == null ? 10 : Math.max(0, first)).map(id -> Data.user(id).orElseThrow()).toList();
    }

    @Field(type = "Club") public int memberCount(Data.Club c) { return c.memberIds().size(); }
    @Field(type = "Club") public Data.Book currentBook(Data.Club c) { return Data.book(c.currentBookId()).orElse(null); }

    @Field(type = "Club")
    public List<Data.Meetup> meetups(Data.Club c, @Arg Boolean upcomingOnly) {
        return Data.meetupsOf(c.id(), upcomingOnly == null || upcomingOnly);
    }

    @Field(type = "User") public List<Data.Club> clubs(Data.User u) { return Data.clubsOf(u.id()); }
    @Field(type = "User", name = "reviews") public List<Data.Review> userReviews(Data.User u) { return Data.reviewsBy(u.id()); }
    @Field(type = "Book") public Double averageRating(Data.Book b) { return Data.averageRating(b.id()); }

    @Field(type = "Book", name = "reviews")
    public List<Data.Review> bookReviews(Data.Book b, @Arg Integer last) {
        return Data.reviewsOf(b.id()).stream().limit(last == null ? 5 : Math.max(0, last)).toList();
    }

    @Field(type = "Review") public Data.User author(Data.Review r) { return Data.user(r.authorId()).orElseThrow(); }
    @Field(type = "Review", name = "book") public Data.Book reviewBook(Data.Review r) { return Data.book(r.bookId()).orElseThrow(); }
    @Field(type = "Meetup", name = "club") public Data.Club meetupClub(Data.Meetup m) { return Data.club(m.clubId()).orElseThrow(); }
}

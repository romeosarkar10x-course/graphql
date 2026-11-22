package dev.shelfie.fed.clubs;

import dev.shelfie.fed.Data;
import dev.shelfie.fed.Data.Club;
import dev.shelfie.fed.Data.User;
import dev.shelfie.fed.Stats;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.graphql.data.federation.EntityMapping;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
@Profile("clubs")
class ClubsController {
    @QueryMapping
    List<Club> clubs() {
        return List.copyOf(Data.CLUBS.values());
    }

    @QueryMapping
    Club club(@Argument String id) {
        return Data.CLUBS.get(id);
    }

    @SchemaMapping(typeName = "Club")
    User owner(Club club) {
        return Data.USERS.get(club.ownerId());
    }

    @SchemaMapping(typeName = "Club")
    List<User> members(Club club) {
        return club.memberIds().stream().map(Data.USERS::get).toList();
    }

    @SchemaMapping(typeName = "Club")
    int memberCount(Club club) {
        return club.memberIds().size();
    }

    @SchemaMapping(typeName = "Club")
    Book currentBook(Club club) {
        return club.currentBookId() == null ? null : new Book(club.currentBookId(), null);
    }

    @EntityMapping
    List<Club> club(@Argument List<String> idList) {
        Stats.entity("Club", idList.size());
        return idList.stream().map(Data.CLUBS::get).toList();
    }

    @EntityMapping
    List<User> user(@Argument List<String> idList) {
        Stats.entity("User", idList.size());
        return idList.stream().map(Data.USERS::get).toList();
    }

    /** A representation for weeksToRead carries the pages the router fetched from catalog first (@requires). */
    @EntityMapping
    List<Book> book(List<Map<String, Object>> representations) {
        Stats.entity("Book", representations.size());
        return representations.stream().map(r -> new Book((String) r.get("id"),
                r.get("pages") instanceof Number n ? n.intValue() : null)).toList();
    }

    @SchemaMapping(typeName = "Book")
    Integer weeksToRead(Book book) {
        return book.pages() == null ? null : (book.pages() + 99) / 100;
    }

    @SchemaMapping(typeName = "Book")
    List<Club> clubsReading(Book book) {
        return Data.CLUBS.values().stream().filter(c -> book.id().equals(c.currentBookId())).toList();
    }

    @SchemaMapping(typeName = "User", field = "clubs")
    List<Club> userClubs(User user) {
        return Data.CLUBS.values().stream().filter(c -> c.memberIds().contains(user.id())).toList();
    }
}

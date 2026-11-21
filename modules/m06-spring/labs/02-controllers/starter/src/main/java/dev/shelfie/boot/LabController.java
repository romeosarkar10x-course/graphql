package dev.shelfie.boot;

import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/** Lab 6.2: the annotations beyond @QueryMapping and @SchemaMapping. */
@Controller
public class LabController {

    /** TODO: OWNER / MEMBER of the club this user was reached through, from local context (@LocalContextValue). */
    @SchemaMapping
    public String roleInClub(Data.User user) { return null; }

    /** TODO: bind titleContains and first to one record with @Arguments; same result as Query.books. */
    @QueryMapping
    public List<Data.Book> bookSearch() { return List.of(); }

    /** TODO: a real patch. This version cannot tell "left out" from "null" (look at ArgumentValue). */
    public record UpdateClubInput(String id, String name, String description) {}

    @MutationMapping
    public Data.Club updateClub(@Argument UpdateClubInput input) {
        var c = Clubs.get(input.id()).orElse(null);
        if (c == null) return null;
        var updated = new Data.Club(c.id(), input.name(), input.description(), c.ownerId(), c.memberIds(),
            c.currentBookId(), c.visibility(), c.createdAt());
        Clubs.put(updated);
        return updated;
    }
}

package dev.shelfie.boot;

import graphql.GraphQLContext;
import graphql.execution.DataFetcherResult;
import java.util.List;
import org.springframework.graphql.data.ArgumentValue;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.Arguments;
import org.springframework.graphql.data.method.annotation.LocalContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/** Lab 6.2: the annotations beyond @QueryMapping and @SchemaMapping. */
@Controller
public class LabController {

    /** Owner and members carry their club down as local context; Spring reads local context as a GraphQLContext. */
    @SchemaMapping(typeName = "Club", field = "owner")
    public DataFetcherResult<Data.User> owner(Data.Club club) {
        return DataFetcherResult.<Data.User>newResult().data(Data.user(club.ownerId()).orElseThrow())
            .localContext(GraphQLContext.of(java.util.Map.of("club", club))).build();
    }

    @SchemaMapping(typeName = "Club", field = "members")
    public DataFetcherResult<List<Data.User>> members(Data.Club club, @Argument int first) {
        var users = club.memberIds().stream().limit(Math.max(0, first)).map(id -> Data.user(id).orElseThrow()).toList();
        return DataFetcherResult.<List<Data.User>>newResult().data(users)
            .localContext(GraphQLContext.of(java.util.Map.of("club", club))).build();
    }

    @SchemaMapping
    public String roleInClub(Data.User user, @LocalContextValue(name = "club", required = false) Data.Club club) {
        if (club == null) return null;
        return club.ownerId().equals(user.id()) ? "OWNER" : "MEMBER";
    }

    /** @Arguments binds the whole argument map to one record. */
    public record BookFilter(String titleContains, Integer first) {}

    @QueryMapping
    public List<Data.Book> bookSearch(@Arguments BookFilter filter) {
        String text = filter.titleContains();
        return Data.BOOKS.stream()
            .filter(b -> text == null || b.title().toLowerCase().contains(text.toLowerCase()))
            .limit(Math.max(0, filter.first() == null ? 20 : filter.first())).toList();
    }

    /** ArgumentValue tells "left out" (isOmitted) from "explicitly null" (present, value null). */
    public record UpdateClubInput(String id, ArgumentValue<String> name, ArgumentValue<String> description) {}

    @MutationMapping
    public Data.Club updateClub(@Argument UpdateClubInput input) {
        var c = Clubs.get(input.id()).orElse(null);
        if (c == null) return null;
        String name = input.name().isOmitted() || input.name().value() == null ? c.name() : input.name().value();
        String description = input.description().isOmitted() ? c.description() : input.description().value();
        var updated = new Data.Club(c.id(), name, description, c.ownerId(), c.memberIds(), c.currentBookId(), c.visibility(), c.createdAt());
        Clubs.put(updated);
        return updated;
    }
}

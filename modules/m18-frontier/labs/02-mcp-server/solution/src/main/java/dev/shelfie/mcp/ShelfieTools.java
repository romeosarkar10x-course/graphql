package dev.shelfie.mcp;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.stereotype.Component;

/**
 * Curated tools: each runs one persisted operation with typed parameters. An agent cannot send its own GraphQL, so
 * it cannot ask for more than these operations select, and every call is one known, priced document.
 */
@Component
public class ShelfieTools {
    private final HttpSyncGraphQlClient shelfie;

    ShelfieTools(HttpSyncGraphQlClient shelfie) {
        this.shelfie = shelfie;
    }

    @Tool(name = "find_books", description = """
            Find books whose title contains a text, ignoring case; without it, list the catalogue in order. Returns
            at most `first` books (default 5) with authors, year and mean rating. There is no author filter: list the
            catalogue and filter yourself.""")
    public Map<String, Object> findBooks(
            @ToolParam(required = false, description = "Part of a title, e.g. \"rose\"") String titleContains,
            @ToolParam(required = false, description = "How many books, default 5") Integer first) {
        Map<String, Object> vars = new LinkedHashMap<>();
        if (titleContains != null) vars.put("titleContains", titleContains);
        if (first != null) vars.put("first", first);
        return run("FindBooks", vars);
    }

    @Tool(name = "book_details", description = """
            One book by id (e.g. b1; ids come from find_books or search): title, authors, year, pages, synopsis,
            mean rating (null without reviews) and its three newest reviews. Null for an unknown id.""")
    public Map<String, Object> bookDetails(@ToolParam(description = "Book id, e.g. b1") String id) {
        return run("BookDetails", Map.of("id", id));
    }

    @Tool(name = "list_clubs", description = """
            Every book club with its visibility (PUBLIC or PRIVATE), member count and the book it is reading now,
            with that book's mean rating. Start here to compare clubs.""")
    public Map<String, Object> listClubs() {
        return run("ListClubs", Map.of());
    }

    @Tool(name = "club_details", description = """
            One club by id (e.g. c1; ids come from list_clubs or search): description, visibility, owner, member
            count, the first 10 members in join order and the current book. Null for an unknown id.""")
    public Map<String, Object> clubDetails(@ToolParam(description = "Club id, e.g. c1") String id) {
        return run("ClubDetails", Map.of("id", id));
    }

    @Tool(name = "club_meetups", description = """
            The meetups of one club: title, start time (UTC, ISO 8601) and location. Only upcoming ones unless
            upcomingOnly is false: pass false for any question about past meetups.""")
    public Map<String, Object> clubMeetups(
            @ToolParam(description = "Club id, e.g. c1") String clubId,
            @ToolParam(required = false, description = "Default true; false includes past meetups") Boolean upcomingOnly) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("clubId", clubId);
        if (upcomingOnly != null) vars.put("upcomingOnly", upcomingOnly);
        return run("ClubMeetups", vars);
    }

    @Tool(name = "reader_profile", description = """
            One reader by user id (e.g. u1; search matches names and handles): name, handle, bio, location and their
            clubs. Null for an unknown id.""")
    public Map<String, Object> readerProfile(@ToolParam(description = "User id, e.g. u1") String id) {
        return run("ReaderProfile", Map.of("id", id));
    }

    @Tool(name = "reader_reviews", description = """
            Every review one reader has written: rating 1 to 5, text, date and the book. Use search first if you
            only know a name.""")
    public Map<String, Object> readerReviews(@ToolParam(description = "User id, e.g. u1") String id) {
        return run("ReaderReviews", Map.of("id", id));
    }

    @Tool(name = "search", description = """
            Search book titles, club names and reader names and handles for a text, ignoring case. Books first,
            then clubs, then readers, each with __typename and id. The quickest way to turn a name into an id.""")
    public Map<String, Object> search(@ToolParam(description = "Text to look for, e.g. \"asha\"") String text) {
        return run("Search", Map.of("text", text));
    }

    /** Run a persisted operation; GraphQL errors become a tool error the agent can read. */
    Map<String, Object> run(String operation, Map<String, Object> variables) {
        ClientGraphQlResponse response = shelfie.documentName(operation).variables(variables).executeSync();
        if (!response.getErrors().isEmpty()) {
            throw new IllegalStateException(operation + " failed: " + response.getErrors().getFirst().getMessage());
        }
        Map<String, Object> data = response.getData();
        return data == null ? Map.of() : data;
    }
}

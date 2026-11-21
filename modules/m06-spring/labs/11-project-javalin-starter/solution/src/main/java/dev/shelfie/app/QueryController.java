package dev.shelfie.app;

import dev.shelfie.tiny.Arg;
import dev.shelfie.tiny.Context;
import dev.shelfie.tiny.Field;
import dev.shelfie.tiny.GraphQLController;
import java.util.List;
import java.util.Map;

@GraphQLController
public class QueryController {
    @Field(type = "Query") public String hello() { return "Hello from Shelfie"; }
    @Field(type = "Query") public Data.Club club(@Arg String id) { return Data.club(id).orElse(null); }
    @Field(type = "Query") public List<Data.Club> clubs() { return Data.CLUBS; }
    @Field(type = "Query") public Data.User user(@Arg String id) { return Data.user(id).orElse(null); }
    @Field(type = "Query") public Data.Book book(@Arg String id) { return Data.book(id).orElse(null); }

    @Field(type = "Query")
    public List<Data.Book> books(@Arg String titleContains, @Arg Integer first) {
        return Data.BOOKS.stream()
            .filter(b -> titleContains == null || b.title().toLowerCase().contains(titleContains.toLowerCase()))
            .limit(first == null ? 20 : Math.max(0, first)).toList();
    }

    @Field(type = "Query")
    public Data.User viewer(@Context("viewer") String viewer) { return viewer == null ? null : Data.user(viewer).orElse(null); }

    @Field(type = "Query") public String boom() { throw new IllegalStateException("boom"); }

    @Field(type = "Mutation")
    public Map<String, Object> createReview(@Arg Map<String, Object> input, @Context("viewer") String viewer) {
        String bookId = (String) input.get("bookId");
        int rating = (Integer) input.get("rating");
        if (viewer == null) return problem(null, "Sign in to review (X-Shelfie-User header)");
        if (Data.book(bookId).isEmpty()) return problem(List.of("input", "bookId"), "No book " + bookId);
        if (rating < 1 || rating > 5) return problem(List.of("input", "rating"), "Rating must be 1 to 5");
        return Map.of("review", Data.addReview(bookId, viewer, rating, (String) input.get("body")), "userErrors", List.of());
    }

    private static Map<String, Object> problem(List<String> field, String message) {
        var error = new java.util.HashMap<String, Object>(Map.of("message", message));
        error.put("field", field);
        var payload = new java.util.HashMap<String, Object>(Map.of("userErrors", List.of(error)));
        payload.put("review", null);
        return payload;
    }
}

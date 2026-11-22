package dev.shelfie.app.graph;

import dev.shelfie.app.service.CatalogService;
import dev.shelfie.app.service.ReviewService;
import dev.shelfie.app.store.Records.Book;
import dev.shelfie.app.store.Records.Club;
import dev.shelfie.app.store.Records.Review;
import dev.shelfie.app.store.Records.User;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/** Root query fields. Controllers only translate between GraphQL and the services. */
@Controller
public class QueryController {

    private final CatalogService catalog;
    private final ReviewService reviews;

    public QueryController(CatalogService catalog, ReviewService reviews) {
        this.catalog = catalog;
        this.reviews = reviews;
    }

    @QueryMapping public String hello() { return "Hello from Shelfie"; }
    @QueryMapping public Club club(@Argument String id) { return catalog.club(id).orElse(null); }
    @QueryMapping public List<Club> clubs() { return catalog.clubs(); }
    @QueryMapping public User user(@Argument String id) { return catalog.user(id).orElse(null); }
    @QueryMapping public Book book(@Argument String id) { return catalog.book(id).orElse(null); }
    @QueryMapping public List<Book> books(@Argument String titleContains, @Argument int first) { return catalog.books(titleContains, first); }
    @QueryMapping public Review review(@Argument String id) { return reviews.review(id); }

    /** ViewerInterceptor puts the X-Shelfie-User header into GraphQLContext as "viewer". */
    @QueryMapping
    public User viewer(@ContextValue(name = "viewer", required = false) String viewer) {
        return viewer == null ? null : catalog.user(viewer).orElse(null);
    }
}

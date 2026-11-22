package dev.shelfie.app.graph;

import dev.shelfie.app.generated.DgsConstants.QUERY;
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

/**
 * Root query fields. Every field and argument name comes from DgsConstants, generated from the schema at
 * build time: rename one in the schema and this class stops compiling.
 */
@Controller
public class QueryController {

    private final CatalogService catalog;
    private final ReviewService reviews;

    public QueryController(CatalogService catalog, ReviewService reviews) {
        this.catalog = catalog;
        this.reviews = reviews;
    }

    @QueryMapping(QUERY.Hello) public String hello() { return "Hello from Shelfie"; }
    @QueryMapping(QUERY.Club) public Club club(@Argument(QUERY.CLUB_INPUT_ARGUMENT.Id) String id) { return catalog.club(id).orElse(null); }
    @QueryMapping(QUERY.Clubs) public List<Club> clubs() { return catalog.clubs(); }
    @QueryMapping(QUERY.User) public User user(@Argument(QUERY.USER_INPUT_ARGUMENT.Id) String id) { return catalog.user(id).orElse(null); }
    @QueryMapping(QUERY.Book) public Book book(@Argument(QUERY.BOOK_INPUT_ARGUMENT.Id) String id) { return catalog.book(id).orElse(null); }

    @QueryMapping(QUERY.Books)
    public List<Book> books(@Argument(QUERY.BOOKS_INPUT_ARGUMENT.TitleContains) String titleContains,
                            @Argument(QUERY.BOOKS_INPUT_ARGUMENT.First) int first) {
        return catalog.books(titleContains, first);
    }

    @QueryMapping(QUERY.Review) public Review review(@Argument(QUERY.REVIEW_INPUT_ARGUMENT.Id) String id) { return reviews.review(id); }

    /** ViewerInterceptor puts the X-Shelfie-User header into GraphQLContext as "viewer". */
    @QueryMapping(QUERY.Viewer)
    public User viewer(@ContextValue(name = "viewer", required = false) String viewer) {
        return viewer == null ? null : catalog.user(viewer).orElse(null);
    }
}

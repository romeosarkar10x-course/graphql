package dev.shelfie.privacy;

import dev.shelfie.privacy.Reviews.Review;
import dev.shelfie.privacy.Users.User;
import graphql.GraphQLContext;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Controller;

@Controller
public class ShelfieController {
    public record Book(String id, String title) {}
    public record AuditEntry(String at, String actor, String operation, String field, List<String> arguments, String outcome) {}
    public record DeleteMyAccountPayload(String erasedUserId, int reviewsDeleted) {}

    static final class Refused extends RuntimeException {
        final ErrorType type;
        Refused(ErrorType type, String message) { super(message); this.type = type; }
    }

    private final JdbcClient jdbc;
    private final Users users;
    private final Reviews reviews;
    ShelfieController(JdbcClient jdbc, Users users, Reviews reviews) {
        this.jdbc = jdbc;
        this.users = users;
        this.reviews = reviews;
    }

    User me(GraphQLContext context) {
        Viewer v = Viewer.of(context);
        User u = v == null ? null : users.byHandle(v.handle());
        if (u == null) throw new Refused(ErrorType.UNAUTHORIZED, "Sign in first");
        return u;
    }

    @QueryMapping public User me(GraphQLContext context, DataFetchingEnvironment env) {
        Viewer v = Viewer.of(context);
        return v == null ? null : users.byHandle(v.handle());
    }

    @QueryMapping public User user(@Argument String id) { return users.byId(id); }

    @QueryMapping public Book book(@Argument String id) {
        return jdbc.sql("SELECT id, title FROM m14_06.books WHERE id = ?").param(id).query(Book.class).optional().orElse(null);
    }

    @QueryMapping public Review review(@Argument String id) { return reviews.byId(id); }

    @QueryMapping public List<AuditEntry> auditLog(@Argument int last, GraphQLContext context) {
        Viewer v = Viewer.of(context);
        if (v == null || !v.admin()) throw new Refused(ErrorType.FORBIDDEN, "Admins only");
        return jdbc.sql("SELECT to_char(at AT TIME ZONE 'UTC', 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"') AS at, actor, operation, field, "
                + "array_to_string(arguments, ',') AS arguments, outcome FROM m14_06.audit_log ORDER BY id DESC LIMIT ?")
            .param(last).query((rs, n) -> new AuditEntry(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getString(5).isEmpty() ? List.of() : List.of(rs.getString(5).split(",")), rs.getString(6))).list();
    }

    @SchemaMapping(typeName = "User") public List<Review> reviews(User user) { return reviews.ofAuthor(user.id()); }
    @SchemaMapping(typeName = "Book") public List<Review> reviews(Book book) { return reviews.ofBook(book.id()); }
    @SchemaMapping public User author(Review review) { return users.byId(review.authorId()); }
    @SchemaMapping public Book book(Review review) { return book(review.bookId()); }

    @MutationMapping public Review createReview(@Argument String bookId, @Argument int rating, @Argument String body, GraphQLContext context) {
        return reviews.create(bookId, me(context).id(), rating, body);
    }

    @MutationMapping public String deleteReview(@Argument String id, GraphQLContext context) {
        mustAuthor(id, context);
        reviews.softDelete(id);
        return id;
    }

    @MutationMapping public Review restoreReview(@Argument String id, GraphQLContext context) {
        mustAuthor(id, context);
        return reviews.restore(id);
    }

    void mustAuthor(String id, GraphQLContext context) {
        String author = reviews.authorOf(id);
        if (author == null) throw new Refused(ErrorType.NOT_FOUND, "No review " + id);
        if (!author.equals(me(context).id())) throw new Refused(ErrorType.FORBIDDEN, "Only the author may do that");
    }

    @MutationMapping public int purgeDeletedReviews(@Argument int olderThanDays, GraphQLContext context) {
        Viewer v = Viewer.of(context);
        if (v == null || !v.admin()) throw new Refused(ErrorType.FORBIDDEN, "Admins only");
        return reviews.purge(olderThanDays);
    }

    @MutationMapping public DeleteMyAccountPayload deleteMyAccount(GraphQLContext context) {
        // TODO a real erase (see the schema): today the account only loses its name.
        User u = me(context);
        jdbc.sql("UPDATE m14_06.users SET name = 'Deleted user' WHERE id = ?").param(u.id()).update();
        return new DeleteMyAccountPayload(u.id(), 0);
    }

    @GraphQlExceptionHandler
    GraphQLError refused(Refused e, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env).errorType(e.type).message(e.getMessage()).build();
    }
}

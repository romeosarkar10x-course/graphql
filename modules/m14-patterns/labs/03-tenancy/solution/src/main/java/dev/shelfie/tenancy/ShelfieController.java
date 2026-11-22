package dev.shelfie.tenancy;

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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Controller;

/**
 * Every resolver reads through TenantDb, so every path (by id, node, search, a shared book's clubs, a mutation)
 * is filtered by the database, not by a WHERE clause each path must remember. Nothing is cached by id alone.
 */
@Controller
public class ShelfieController {
    public record Viewer(String handle, String tenant, String plan) {}
    public record Club(String id, String name, String currentBookId) {}
    public record Book(String id, String title) {}
    public record ClubAnalytics(int memberCount) {}
    public record UserError(String message) {}
    public record JoinClubPayload(Club club, List<UserError> userErrors) {}

    private final TenantDb db;
    private final Plans plans;

    ShelfieController(TenantDb db, Plans plans) {
        this.db = db;
        this.plans = plans;
    }

    static final String CLUB = "SELECT id, name, current_book_id FROM m14_03.clubs ";

    static Club club(JdbcClient jdbc, String id) {
        return jdbc.sql(CLUB + "WHERE id = ?").param(id).query(Club.class).optional().orElse(null);
    }

    @QueryMapping public Viewer viewer(GraphQLContext context) {
        Tenant t = Tenant.of(context);
        return new Viewer(t.handle(), t.id(), plans.of(t));
    }

    @QueryMapping public Club club(@Argument String id, GraphQLContext context) {
        return db.as(Tenant.of(context), jdbc -> club(jdbc, id));
    }

    @QueryMapping public List<Club> clubs(GraphQLContext context) {
        return db.as(Tenant.of(context), jdbc -> jdbc.sql(CLUB + "ORDER BY id").query(Club.class).list());
    }

    @QueryMapping public Object node(@Argument String id, GraphQLContext context) {
        Tenant t = Tenant.of(context);
        if (id.startsWith("c-")) return db.as(t, jdbc -> club(jdbc, id));
        if (id.startsWith("b")) return book(id, context);
        return null;
    }

    @QueryMapping public List<Club> search(@Argument String text, GraphQLContext context) {
        return db.as(Tenant.of(context), jdbc -> jdbc.sql(CLUB + "WHERE name ILIKE '%' || ? || '%' ORDER BY id").param(text).query(Club.class).list());
    }

    @QueryMapping public Book book(@Argument String id, GraphQLContext context) {
        return db.as(Tenant.of(context), jdbc -> jdbc.sql("SELECT id, title FROM m14_03.books WHERE id = ?").param(id).query(Book.class).optional().orElse(null));
    }

    @SchemaMapping public List<Club> clubsReading(Book book, GraphQLContext context) {
        return db.as(Tenant.of(context), jdbc -> jdbc.sql(CLUB + "WHERE current_book_id = ? ORDER BY id").param(book.id()).query(Club.class).list());
    }

    @SchemaMapping public List<String> members(Club club, GraphQLContext context) {
        return db.as(Tenant.of(context), jdbc -> jdbc.sql("SELECT handle FROM m14_03.members WHERE club_id = ? ORDER BY joined").param(club.id()).query(String.class).list());
    }

    @SchemaMapping public Book currentBook(Club club, GraphQLContext context) {
        return club.currentBookId() == null ? null : book(club.currentBookId(), context);
    }

    @SchemaMapping public ClubAnalytics analytics(Club club, GraphQLContext context) {
        return new ClubAnalytics(members(club, context).size());
    }

    @MutationMapping public JoinClubPayload joinClub(@Argument String clubId, GraphQLContext context) {
        Tenant t = Tenant.of(context);
        return db.as(t, jdbc -> {
            Club club = club(jdbc, clubId);
            // Same answer for "another tenant's" and "no such club": no oracle for guessing ids.
            if (club == null) return new JoinClubPayload(null, List.of(new UserError("No club " + clubId)));
            jdbc.sql("INSERT INTO m14_03.members (tenant_id, club_id, handle) VALUES (?, ?, ?) ON CONFLICT DO NOTHING")
                .params(t.id(), clubId, t.handle()).update();
            return new JoinClubPayload(club, List.of());
        });
    }

    @GraphQlExceptionHandler
    GraphQLError notSignedIn(Tenant.NotSignedIn e, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError(env).errorType(e.type()).message(e.getMessage()).build();
    }
}

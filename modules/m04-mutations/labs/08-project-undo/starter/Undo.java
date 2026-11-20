import graphql.GraphQL;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mini-project 4: undo everything, with the command pattern (README). The mutations work; none of
 * them can be undone yet. Every graphQL() call is a fresh, seeded service.
 */
public final class Undo {
    private Undo() {}

    record Club(String id, String name, List<String> memberIds) {}
    record Review(String id, String bookId, String authorId, int rating, String body) {}

    static final class State {
        final Map<String, Club> clubs = new LinkedHashMap<>();
        final Map<String, Review> reviews = new LinkedHashMap<>();
        int nextReview = 16;
        State() {
            clubs.put("c1", new Club("c1", "The Midnight Readers", List.of("u11", "u1", "u2")));
            clubs.put("c2", new Club("c2", "Slow Burn Mysteries", List.of("u7", "u5")));
        }
    }

    // TODO 1: a sealed Command interface with one record per change (rename, join, leave, create,
    // delete). Each can apply itself, produce its inverse, and say whether the inverse is still safe.

    static final class Service {
        final State state = new State();

        // TODO 2: hand out a token per applied command; undo(token) applies the inverse once,
        // refuses (CONFLICT) when the state moved on, and returns a token for the redo.
        synchronized Map<String, Object> undo(String token) {
            return errors("No such undo token.", "NOT_FOUND");
        }
    }

    static Map<String, Object> errors(String message, String code) {
        return Map.of("userErrors", List.of(Map.of("message", message, "code", code)));
    }

    /** Changes the state and returns the undo token: none yet. */
    static String apply(Service s, Runnable change) {
        change.run();
        return null;
    }

    static Map<String, Object> clubPayload(Service s, String clubId, String token) {
        Map<String, Object> p = new HashMap<>();
        p.put("club", s.state.clubs.get(clubId));
        p.put("undoToken", token);
        p.put("userErrors", List.of());
        return p;
    }

    static Map<String, Object> payload(String key, Object value, String token) {
        Map<String, Object> p = new HashMap<>();
        p.put(key, value);
        p.put("undoToken", token);
        p.put("userErrors", List.of());
        return p;
    }

    static String viewer(DataFetchingEnvironment env) {
        return env.getGraphQlContext().get("viewer");
    }

    public static GraphQL graphQL() {
        Service s = new Service();
        var wiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", t -> t
                        .dataFetcher("club", env -> s.state.clubs.get(env.<String>getArgument("id")))
                        .dataFetcher("review", env -> s.state.reviews.get(env.<String>getArgument("id"))))
                .type("Mutation", t -> t
                        .dataFetcher("renameClub", env -> {
                            Map<String, Object> in = env.getArgument("input");
                            synchronized (s) {
                                Club c = s.state.clubs.get((String) in.get("clubId"));
                                if (c == null) return errors("No such club.", "NOT_FOUND");
                                if (c.name().equals(in.get("name"))) return clubPayload(s, c.id(), null);
                                return clubPayload(s, c.id(), apply(s, () -> s.state.clubs.put(c.id(), new Club(c.id(), (String) in.get("name"), c.memberIds()))));
                            }
                        })
                        .dataFetcher("joinClub", env -> {
                            Map<String, Object> in = env.getArgument("input");
                            synchronized (s) {
                                Club c = s.state.clubs.get((String) in.get("clubId"));
                                if (c == null) return errors("No such club.", "NOT_FOUND");
                                if (c.memberIds().contains(viewer(env))) return clubPayload(s, c.id(), null);
                                return clubPayload(s, c.id(), apply(s, () -> { List<String> m = new ArrayList<>(c.memberIds()); m.add(viewer(env)); s.state.clubs.put(c.id(), new Club(c.id(), c.name(), List.copyOf(m))); }));
                            }
                        })
                        .dataFetcher("leaveClub", env -> {
                            Map<String, Object> in = env.getArgument("input");
                            synchronized (s) {
                                Club c = s.state.clubs.get((String) in.get("clubId"));
                                if (c == null) return errors("No such club.", "NOT_FOUND");
                                if (!c.memberIds().contains(viewer(env))) return clubPayload(s, c.id(), null);
                                return clubPayload(s, c.id(), apply(s, () -> { List<String> m = new ArrayList<>(c.memberIds()); m.remove(viewer(env)); s.state.clubs.put(c.id(), new Club(c.id(), c.name(), List.copyOf(m))); }));
                            }
                        })
                        .dataFetcher("createReview", env -> {
                            Map<String, Object> in = env.getArgument("input");
                            synchronized (s) {
                                Review r = new Review("r" + s.state.nextReview++, (String) in.get("bookId"), viewer(env),
                                        (Integer) in.get("rating"), (String) in.get("body"));
                                return payload("review", r, apply(s, () -> s.state.reviews.put(r.id(), r)));
                            }
                        })
                        .dataFetcher("deleteReview", env -> {
                            Map<String, Object> in = env.getArgument("input");
                            synchronized (s) {
                                Review r = s.state.reviews.get((String) in.get("reviewId"));
                                if (r == null) return errors("No such review.", "NOT_FOUND");
                                if (!r.authorId().equals(viewer(env))) return errors("Only the author can delete a review.", "FORBIDDEN");
                                return payload("review", r, apply(s, () -> s.state.reviews.remove(r.id())));
                            }
                        })
                        .dataFetcher("undo", env -> s.undo((String) env.<Map<String, Object>>getArgument("input").get("token"))))
                .build();
        try {
            return GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(
                    new SchemaParser().parse(Files.readString(Path.of("undo.graphqls"))), wiring)).build();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

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
import java.util.Optional;
import java.util.UUID;

/**
 * Mini-project 4: undo everything, with the command pattern. A mutation builds a Command, applies it,
 * and stores it under a fresh token; undo(token) applies the command's inverse, if the state still is
 * what the command left behind. Every graphQL() call is a fresh, seeded service.
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

    /** A reversible change. Each knows how to check that it is still the latest word on its data. */
    sealed interface Command {
        String describe();
        Optional<String> conflict(State s);   // empty when the inverse can be applied safely
        void apply(State s);
        Command inverse();
    }

    record Rename(String clubId, String from, String to) implements Command {
        public String describe() { return "rename " + clubId; }
        public Optional<String> conflict(State s) {
            return s.clubs.get(clubId).name().equals(to) ? Optional.empty()
                    : Optional.of("The club was renamed again since (now '" + s.clubs.get(clubId).name() + "').");
        }
        public void apply(State s) { Club c = s.clubs.get(clubId); s.clubs.put(clubId, new Club(clubId, to, c.memberIds())); }
        public Command inverse() { return new Rename(clubId, to, from); }
    }

    record Join(String clubId, String userId) implements Command {
        public String describe() { return "join " + clubId; }
        public Optional<String> conflict(State s) {
            return s.clubs.get(clubId).memberIds().contains(userId) ? Optional.empty() : Optional.of(userId + " already left " + clubId + ".");
        }
        public void apply(State s) {
            Club c = s.clubs.get(clubId);
            List<String> m = new ArrayList<>(c.memberIds());
            m.add(userId);
            s.clubs.put(clubId, new Club(clubId, c.name(), List.copyOf(m)));
        }
        public Command inverse() { return new Leave(clubId, userId); }
    }

    record Leave(String clubId, String userId) implements Command {
        public String describe() { return "leave " + clubId; }
        public Optional<String> conflict(State s) {
            return s.clubs.get(clubId).memberIds().contains(userId) ? Optional.of(userId + " rejoined " + clubId + ".") : Optional.empty();
        }
        public void apply(State s) {
            Club c = s.clubs.get(clubId);
            List<String> m = new ArrayList<>(c.memberIds());
            m.remove(userId);
            s.clubs.put(clubId, new Club(clubId, c.name(), List.copyOf(m)));
        }
        public Command inverse() { return new Join(clubId, userId); }
    }

    record Create(Review review) implements Command {
        public String describe() { return "create " + review.id(); }
        public Optional<String> conflict(State s) {
            return review.equals(s.reviews.get(review.id())) ? Optional.empty() : Optional.of("Review " + review.id() + " was deleted or changed since.");
        }
        public void apply(State s) { s.reviews.put(review.id(), review); }
        public Command inverse() { return new Delete(review); }
    }

    record Delete(Review review) implements Command {
        public String describe() { return "delete " + review.id(); }
        public Optional<String> conflict(State s) {
            return s.reviews.containsKey(review.id()) ? Optional.of("Review " + review.id() + " exists again.") : Optional.empty();
        }
        public void apply(State s) { s.reviews.remove(review.id()); }
        public Command inverse() { return new Create(review); }
    }

    static final class Service {
        final State state = new State();
        final Map<String, Command> tokens = new HashMap<>();
        final java.util.Set<String> used = new java.util.HashSet<>();

        /** Apply a command and hand out the token that undoes it. */
        synchronized String run(Command c) {
            c.apply(state);
            String token = "undo-" + UUID.randomUUID();
            tokens.put(token, c);
            return token;
        }

        synchronized Map<String, Object> undo(String token) {
            if (used.contains(token)) return errors("This token was already used.", "USED");
            Command c = tokens.get(token);
            if (c == null) return errors("No such undo token.", "NOT_FOUND");
            Optional<String> conflict = c.conflict(state);
            if (conflict.isPresent()) return errors(conflict.get(), "CONFLICT");
            used.add(token);
            tokens.remove(token);
            return Map.of("undone", c.describe(), "undoToken", run(c.inverse()), "userErrors", List.of());
        }
    }

    static Map<String, Object> errors(String message, String code) {
        return Map.of("userErrors", List.of(Map.of("message", message, "code", code)));
    }

    static Map<String, Object> clubPayload(Service s, String clubId, String token) {
        Map<String, Object> p = new HashMap<>();
        p.put("club", s.state.clubs.get(clubId));
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
                                return clubPayload(s, c.id(), s.run(new Rename(c.id(), c.name(), (String) in.get("name"))));
                            }
                        })
                        .dataFetcher("joinClub", env -> {
                            Map<String, Object> in = env.getArgument("input");
                            synchronized (s) {
                                Club c = s.state.clubs.get((String) in.get("clubId"));
                                if (c == null) return errors("No such club.", "NOT_FOUND");
                                if (c.memberIds().contains(viewer(env))) return clubPayload(s, c.id(), null);
                                return clubPayload(s, c.id(), s.run(new Join(c.id(), viewer(env))));
                            }
                        })
                        .dataFetcher("leaveClub", env -> {
                            Map<String, Object> in = env.getArgument("input");
                            synchronized (s) {
                                Club c = s.state.clubs.get((String) in.get("clubId"));
                                if (c == null) return errors("No such club.", "NOT_FOUND");
                                if (!c.memberIds().contains(viewer(env))) return clubPayload(s, c.id(), null);
                                return clubPayload(s, c.id(), s.run(new Leave(c.id(), viewer(env))));
                            }
                        })
                        .dataFetcher("createReview", env -> {
                            Map<String, Object> in = env.getArgument("input");
                            synchronized (s) {
                                Review r = new Review("r" + s.state.nextReview++, (String) in.get("bookId"), viewer(env),
                                        (Integer) in.get("rating"), (String) in.get("body"));
                                return Map.of("review", r, "undoToken", s.run(new Create(r)), "userErrors", List.of());
                            }
                        })
                        .dataFetcher("deleteReview", env -> {
                            Map<String, Object> in = env.getArgument("input");
                            synchronized (s) {
                                Review r = s.state.reviews.get((String) in.get("reviewId"));
                                if (r == null) return errors("No such review.", "NOT_FOUND");
                                if (!r.authorId().equals(viewer(env))) return errors("Only the author can delete a review.", "FORBIDDEN");
                                return Map.of("review", r, "undoToken", s.run(new Delete(r)), "userErrors", List.of());
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

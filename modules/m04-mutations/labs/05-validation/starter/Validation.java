import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lab 4.5: four places a bad input can be stopped, and what the client sees from each.
 *
 *   java -cp "$(lib)" Validation.java cases.jsonl     one line per case: "<case> <layer> <detail>"
 */
public class Validation {

    static final Set<String> BOOKS = Set.of("b1", "b2", "b3", "b4", "b5", "b6", "b7", "b8", "b9", "b10");
    static final Set<String> ALREADY_REVIEWED_BY_U1 = Set.of("b1", "b2", "b8");

    record CreateReviewInput(String bookId, int rating, String body, List<String> tags) {}

    /**
     * First draft: every rule in the resolver, by hand. It works, and that is the problem: clients
     * cannot see these rules in the schema, and each new mutation repeats them.
     * TODO: move shape rules to schema directives, Java-only rules to Bean Validation on the record,
     * and keep only facts about the world (does the book exist, did u1 review it) here.
     */
    @SuppressWarnings("unchecked")
    static Object createReview(Map<String, Object> in) {
        var input = new CreateReviewInput((String) in.get("bookId"), (Integer) in.get("rating"), (String) in.get("body"),
                (List<String>) in.getOrDefault("tags", List.of()));
        List<Map<String, Object>> errors = new ArrayList<>();
        if (input.rating() < 1 || input.rating() > 5) errors.add(invalid("rating", "Rating must be 1 to 5."));
        if (input.body().length() < 10 || input.body().length() > 2000) errors.add(invalid("body", "Body must be 10 to 2000 characters."));
        if (input.tags().size() > 5) errors.add(invalid("tags", "At most 5 tags."));
        if (!errors.isEmpty()) return Map.of("userErrors", errors);
        if (!BOOKS.contains(input.bookId())) {
            return Map.of("userErrors", List.of(Map.of("field", List.of("input", "bookId"), "message", "No such book.", "code", "NOT_FOUND")));
        }
        if (ALREADY_REVIEWED_BY_U1.contains(input.bookId())) {
            return Map.of("userErrors", List.of(Map.of("field", List.of("input", "bookId"), "message", "You already reviewed this book.", "code", "DUPLICATE")));
        }
        return Map.of("review", Map.of("id", "r16", "bookId", input.bookId(), "rating", input.rating(), "body", input.body(), "tags", input.tags()),
                "userErrors", List.of());
    }

    static Map<String, Object> invalid(String field, String message) {
        return Map.of("field", List.of("input", field), "message", message, "code", "INVALID");
    }

    static GraphQL graphQL() throws Exception {
        var wiring = RuntimeWiring.newRuntimeWiring()
                .type("Mutation", t -> t.dataFetcher("createReview", env -> createReview(env.getArgument("input"))))
                .build();
        return GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(
                new SchemaParser().parse(Files.readString(Path.of("schema.graphqls"))), wiring)).build();
    }

    /** Which layer stopped the request, from what the client received. */
    @SuppressWarnings("unchecked")
    static String layer(Map<String, Object> spec) {
        List<Map<String, Object>> errors = (List<Map<String, Object>>) spec.getOrDefault("errors", List.of());
        if (!errors.isEmpty()) {
            List<String> parts = new ArrayList<>();
            for (var e : errors) {
                Object c = ((Map<String, Object>) e.getOrDefault("extensions", Map.of())).get("classification");
                if (c instanceof Map<?, ?> m && "ExtendedValidationError".equals(m.get("type"))) parts.add("directive " + m.get("constraint"));
                else if (String.valueOf(e.get("message")).startsWith("Validation error")) parts.add("type validation");
                else if ("ValidationError".equals(c)) parts.add("type variable-coercion");
                else parts.add("error " + c);
            }
            return String.join(", ", parts.stream().distinct().toList());
        }
        Map<String, Object> payload = (Map<String, Object>) ((Map<String, Object>) spec.get("data")).get("createReview");
        List<Map<String, Object>> userErrors = (List<Map<String, Object>>) payload.get("userErrors");
        if (userErrors.isEmpty()) return "ok";
        List<String> parts = new ArrayList<>();
        for (var u : userErrors) {
            String code = (String) u.get("code");
            String field = String.join(".", (List<String>) u.get("field"));
            parts.add(code.equals("INVALID") ? "bean " + field : "business " + code);
        }
        return String.join(", ", parts);
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        GraphQL g = graphQL();
        for (String line : Files.readAllLines(Path.of(args[0]))) {
            if (line.isBlank()) continue;
            Map<String, Object> c = tools.jackson.databind.json.JsonMapper.builder().build().readValue(line, LinkedHashMap.class);
            String doc = (String) c.getOrDefault("query",
                    "mutation($input: CreateReviewInput!) { createReview(input: $input) { review { id } userErrors { field message code } } }");
            Map<String, Object> vars = c.containsKey("input") ? new LinkedHashMap<>() : Map.of();
            if (c.containsKey("input")) vars.put("input", c.get("input"));
            var r = g.execute(ExecutionInput.newExecutionInput(doc).variables(vars).build());
            System.out.println(c.get("case") + " " + layer(r.toSpecification()));
        }
    }
}

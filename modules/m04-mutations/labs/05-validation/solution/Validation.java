import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.validation.rules.OnValidationErrorStrategy;
import graphql.validation.rules.ValidationRules;
import graphql.validation.schemawiring.ValidationSchemaWiring;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
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

    /** Layer 3: rules that need Java, on the input record. */
    record CreateReviewInput(String bookId, int rating, @NotBlank String body, List<String> tags) {
        @AssertTrue(message = "tags must not repeat")
        public boolean isTagsUnique() {
            return tags == null || new HashSet<>(tags).size() == tags.size();
        }
    }

    static final Validator BEAN = jakarta.validation.Validation.byDefaultProvider().configure()
            .messageInterpolator(new org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator())
            .buildValidatorFactory().getValidator();

    @SuppressWarnings("unchecked")
    static Object createReview(Map<String, Object> in) {
        var input = new CreateReviewInput((String) in.get("bookId"), (Integer) in.get("rating"), (String) in.get("body"),
                (List<String>) in.getOrDefault("tags", List.of()));
        List<Map<String, Object>> errors = new ArrayList<>();
        // Layer 3: Bean Validation
        for (ConstraintViolation<CreateReviewInput> v : BEAN.validate(input).stream()
                .sorted(Comparator.comparing(v -> v.getPropertyPath().toString())).toList()) {
            String property = v.getPropertyPath().iterator().next().getName();
            errors.add(Map.of("field", List.of("input", property), "message", v.getMessage(), "code", "INVALID"));
        }
        if (!errors.isEmpty()) return Map.of("userErrors", errors);
        // Layer 4: business rules, facts about the world
        if (!BOOKS.contains(input.bookId())) {
            return Map.of("userErrors", List.of(Map.of("field", List.of("input", "bookId"), "message", "No such book.", "code", "NOT_FOUND")));
        }
        if (ALREADY_REVIEWED_BY_U1.contains(input.bookId())) {
            return Map.of("userErrors", List.of(Map.of("field", List.of("input", "bookId"), "message", "You already reviewed this book.", "code", "DUPLICATE")));
        }
        return Map.of("review", Map.of("id", "r16", "bookId", input.bookId(), "rating", input.rating(), "body", input.body(), "tags", input.tags()),
                "userErrors", List.of());
    }

    static GraphQL graphQL() throws Exception {
        // Layer 2: schema directives, checked by graphql-java-extended-validation before the resolver runs
        var rules = ValidationRules.newValidationRules().onValidationErrorStrategy(OnValidationErrorStrategy.RETURN_NULL).build();
        var wiring = RuntimeWiring.newRuntimeWiring()
                .directiveWiring(new ValidationSchemaWiring(rules))
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

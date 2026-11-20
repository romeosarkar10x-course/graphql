import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Lab 3.3: a custom ISBN scalar, plus four from graphql-java-extended-scalars.
 *
 *   java -cp "$(lib)" Isbn.java query.graphql ['{...}']   run a query
 *   java -cp "$(lib)" Isbn.java table inputs.json         try each input as a literal and as a variable
 */
public class Isbn {

    // ---------------------------------------------------------------- the scalar

    /**
     * TODO 1: make this right (README has the rules). This first draft has no checksum, keeps
     * ISBN-10s as they are, and strips characters it should reject.
     */
    static Optional<String> normalise(String raw) {
        String s = raw.replaceAll("[^0-9Xx]", "");
        return s.length() == 13 || s.length() == 10 ? Optional.of(s) : Optional.empty();
    }

    static final GraphQLScalarType ISBN = GraphQLScalarType.newScalar()
            .name("ISBN")
            .description("ISBN-13 as 13 digits")
            .coercing(new Coercing<String, String>() {
                @Override
                public String serialize(Object out, GraphQLContext ctx, Locale locale) {
                    return String.valueOf(out);
                }

                @Override
                public String parseValue(Object in, GraphQLContext ctx, Locale locale) {
                    return normalise(String.valueOf(in)).orElseThrow(() -> new CoercingParseValueException("Not a valid ISBN: " + in));
                }

                @Override
                public String parseLiteral(Value<?> in, CoercedVariables vars, GraphQLContext ctx, Locale locale) {
                    if (in instanceof StringValue sv) return sv.getValue();
                    throw new CoercingParseLiteralException("ISBN must be a string literal");
                }
            })
            .build();

    // ---------------------------------------------------------------- the schema

    static final Map<String, String> PRICES = Map.of("b1", "12.99", "b2", "10.50", "b6", "18.00");

    // TODO 2: register the extended scalars (DateTime, BigDecimal, Url) and wire Book.coverUrl,
    // Book.listPrice (PRICES, null when absent) and Review.createdAt (parse the seed's String).
    static RuntimeWiring wiring() {
        return RuntimeWiring.newRuntimeWiring()
                .scalar(ISBN)
                .type("Query", t -> t
                        .dataFetcher("book", env -> Data.BOOKS.stream().filter(b -> b.isbn().equals(env.getArgument("isbn"))).findFirst().orElse(null))
                        .dataFetcher("check", env -> env.getArgument("isbn"))
                        .dataFetcher("books", env -> Data.BOOKS))
                .type("Book", t -> t
                        .dataFetcher("reviews", env -> Data.reviewsOf(env.<Data.Book>getSource().id())))
                .build();
    }

    static GraphQL graphQL() throws Exception {
        return GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(
                new SchemaParser().parse(Files.readString(Path.of("schema.graphqls"))), wiring())).build();
    }

    // ---------------------------------------------------------------- the table

    /** A JSON value written as a GraphQL literal. */
    static String literal(Object v) {
        if (v == null) return "null";
        if (v instanceof String s) return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
        if (v instanceof List<?> l) return "[" + String.join(", ", l.stream().map(Isbn::literal).toList()) + "]";
        if (v instanceof Map<?, ?> m) return "{" + String.join(", ", m.entrySet().stream().map(e -> e.getKey() + ": " + literal(e.getValue())).toList()) + "}";
        return v.toString();
    }

    /** "ok <value>" or "rejected/<stage>": validation (literals), variable-coercion (variables) or execution. */
    @SuppressWarnings("unchecked")
    static String outcome(Map<String, Object> spec) {
        if (spec.get("errors") instanceof List<?> errs && !errs.isEmpty()) {
            String msg = String.valueOf(((Map<String, Object>) errs.get(0)).get("message"));
            String stage = msg.startsWith("Validation error") ? "validation"
                    : msg.startsWith("Variable '") ? "variable-coercion" : "execution";
            return "rejected/" + stage;
        }
        return "ok " + ((Map<String, Object>) spec.get("data")).get("check");
    }

    @SuppressWarnings("unchecked")
    static void table(Path inputs) throws Exception {
        GraphQL g = graphQL();
        for (Object row : Json.MAPPER.readValue(Files.readString(inputs), List.class)) {
            Map<String, Object> r = (Map<String, Object>) row;
            Object value = r.get("value");
            var lit = g.execute("{ check(isbn: " + literal(value) + ") }").toSpecification();
            Map<String, Object> vars = new java.util.HashMap<>();
            vars.put("i", value);
            var var = g.execute(ExecutionInput.newExecutionInput("query($i: ISBN!) { check(isbn: $i) }").variables(vars).build()).toSpecification();
            System.out.println(r.get("label") + "\t" + outcome(lit) + "\t" + outcome(var));
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length >= 2 && args[0].equals("table")) {
            table(Path.of(args[1]));
            return;
        }
        Map<String, Object> vars = args.length > 1 ? Json.read(args[1]) : Map.of();
        var result = graphQL().execute(ExecutionInput.newExecutionInput(Files.readString(Path.of(args[0]))).variables(vars).build());
        System.out.println(Json.of(result.toSpecification()));
    }
}

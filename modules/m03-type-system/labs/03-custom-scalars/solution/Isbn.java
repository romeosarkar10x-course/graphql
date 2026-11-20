import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.scalars.ExtendedScalars;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
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

    /** Normalises to 13 ASCII digits or returns empty. Hyphens and ASCII spaces are ignored. */
    static Optional<String> normalise(String raw) {
        String s = raw.replace("-", "").replace(" ", "");
        if (s.length() == 13 && allAsciiDigits(s) && (s.startsWith("978") || s.startsWith("979"))) {
            return check13(s.substring(0, 12)) == s.charAt(12) - '0' ? Optional.of(s) : Optional.empty();
        }
        if (s.length() == 10 && allAsciiDigits(s.substring(0, 9))) {
            char last = Character.toUpperCase(s.charAt(9));
            int sum = 0;
            for (int i = 0; i < 9; i++) sum += (10 - i) * (s.charAt(i) - '0');
            int check = (11 - sum % 11) % 11;
            boolean ok = check == 10 ? last == 'X' : last == (char) ('0' + check);
            if (!ok) return Optional.empty();
            String body = "978" + s.substring(0, 9);
            return Optional.of(body + check13(body));
        }
        return Optional.empty();
    }

    /** Character.isDigit would accept Arabic-Indic or full-width digits; ISBNs are ASCII. */
    static boolean allAsciiDigits(String s) {
        return !s.isEmpty() && s.chars().allMatch(c -> c >= '0' && c <= '9');
    }

    static int check13(String twelve) {
        int sum = 0;
        for (int i = 0; i < 12; i++) sum += (twelve.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        return (10 - sum % 10) % 10;
    }

    static final GraphQLScalarType ISBN = GraphQLScalarType.newScalar()
            .name("ISBN")
            .description("ISBN-13 as 13 digits")
            .coercing(new Coercing<String, String>() {
                @Override
                public String serialize(Object out, GraphQLContext ctx, Locale locale) {
                    if (out instanceof String s) {
                        return normalise(s).orElseThrow(() -> new CoercingSerializeException("Not a valid ISBN: '" + s + "'"));
                    }
                    throw new CoercingSerializeException("ISBN must be a String, got " + out.getClass().getSimpleName());
                }

                @Override
                public String parseValue(Object in, GraphQLContext ctx, Locale locale) {
                    if (in instanceof String s) {
                        return normalise(s).orElseThrow(() -> new CoercingParseValueException("Not a valid ISBN: '" + s + "'"));
                    }
                    throw new CoercingParseValueException("ISBN must be a String, got " + in.getClass().getSimpleName());
                }

                @Override
                public String parseLiteral(Value<?> in, CoercedVariables vars, GraphQLContext ctx, Locale locale) {
                    if (in instanceof StringValue sv) {
                        return normalise(sv.getValue()).orElseThrow(() -> new CoercingParseLiteralException("Not a valid ISBN: '" + sv.getValue() + "'"));
                    }
                    throw new CoercingParseLiteralException("ISBN must be a string literal, got " + in.getClass().getSimpleName());
                }
            })
            .build();

    // ---------------------------------------------------------------- the schema

    static final Map<String, String> PRICES = Map.of("b1", "12.99", "b2", "10.50", "b6", "18.00");

    static RuntimeWiring wiring() {
        return RuntimeWiring.newRuntimeWiring()
                .scalar(ISBN)
                .scalar(ExtendedScalars.DateTime)
                .scalar(ExtendedScalars.GraphQLBigDecimal)
                .scalar(ExtendedScalars.Url)
                .type("Query", t -> t
                        .dataFetcher("book", env -> Data.BOOKS.stream().filter(b -> b.isbn().equals(env.getArgument("isbn"))).findFirst().orElse(null))
                        .dataFetcher("check", env -> env.getArgument("isbn"))
                        .dataFetcher("books", env -> Data.BOOKS))
                .type("Book", t -> t
                        .dataFetcher("coverUrl", env -> URI.create("https://cdn.shelfie.dev/covers/" + env.<Data.Book>getSource().isbn() + ".jpg"))
                        .dataFetcher("listPrice", env -> Optional.ofNullable(PRICES.get(env.<Data.Book>getSource().id())).map(BigDecimal::new).orElse(null))
                        .dataFetcher("reviews", env -> Data.reviewsOf(env.<Data.Book>getSource().id())))
                .type("Review", t -> t
                        .dataFetcher("createdAt", env -> OffsetDateTime.parse(env.<Data.Review>getSource().createdAt())))
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

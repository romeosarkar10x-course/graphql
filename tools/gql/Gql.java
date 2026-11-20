/*
 * Course helper built on graphql-java 26.1. Run it as `bin/course gql <command> ...`.
 *
 *   validate <doc.graphql> [--schema <file.graphqls | http url>]
 *       Parse and validate an operation document against a schema, without executing it.
 *       Default schema: $SHELFIE_URL/graphql/schema (the reference server's SDL).
 *       Works for subscriptions too, which the reference server's HTTP endpoint refuses to run.
 *   operations <doc.graphql>
 *       List the operations in a document, one per line: "<type> <name or (anonymous)> uses=<features>",
 *       where features are alias, fragment-spread, inline-fragment, directive, variables.
 *   schema-lines <file.graphqls | dir | url>
 *       Print a schema as sorted one-line facts ("Club.owner: User!", "union SearchResult = ...").
 *       Two schemas are equivalent when their lines are equal, whatever the order or formatting.
 *   coordinates <schema> <coordinate>...      (Module 3.9)
 *       Resolve schema coordinates (September 2025 spec, "Schema Coordinates"): Type, Type.field,
 *       Type.field(arg:), Enum.VALUE, Input.field, @directive, @directive(arg:). Prints
 *       "ok <coordinate> <kind>" or "missing <coordinate>"; exit 1 if any is missing.
 *   stats <schema>                            (Module 3.9)
 *       JSON counts: named types (no built-ins), deprecated fields/arguments/input fields/enum values.
 *
 * A <schema> that is a directory means every *.graphqls file in it, merged (type extensions allowed).
 *
 * Exit status: 0 ok, 1 invalid document, 2 usage or I/O problem.
 */
import graphql.ParseAndValidate;
import graphql.language.Document;
import graphql.parser.Parser;
import graphql.schema.*;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.UnExecutableSchemaGenerator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Gql {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) usage();
        switch (args[0]) {
            case "validate" -> {
                String schemaSource = System.getenv().getOrDefault("SHELFIE_URL", "http://shelfie-ref:8080") + "/graphql/schema";
                if (args.length == 4 && args[2].equals("--schema")) schemaSource = args[3];
                else if (args.length != 2) usage();
                System.exit(validate(Files.readString(Path.of(args[1])), schema(schemaSource)));
            }
            case "schema-lines" -> schemaLines(schema(args[1])).forEach(System.out::println);
            case "operations" -> System.exit(operations(Files.readString(Path.of(args[1]))));
            case "coordinates" -> System.exit(coordinates(schema(args[1]), List.of(args).subList(2, args.length)));
            case "stats" -> System.out.println(stats(schema(args[1])));
            default -> usage();
        }
    }

    static int validate(String text, GraphQLSchema schema) {
        Document doc;
        try {
            doc = Parser.parse(text);
        } catch (Exception e) {
            System.out.println("syntax error: " + e.getMessage());
            return 1;
        }
        var errors = ParseAndValidate.validate(schema, doc);
        if (errors.isEmpty()) {
            System.out.println("valid");
            return 0;
        }
        errors.forEach(e -> System.out.println(e.getMessage() + "  " + e.getLocations()));
        return 1;
    }

    static int operations(String text) {
        Document doc;
        try {
            doc = Parser.parse(text);
        } catch (Exception e) {
            System.out.println("syntax error: " + e.getMessage());
            return 1;
        }
        for (var op : doc.getDefinitionsOfType(graphql.language.OperationDefinition.class)) {
            var uses = new java.util.TreeSet<String>();
            if (!op.getVariableDefinitions().isEmpty()) uses.add("variables");
            new graphql.language.NodeTraverser().depthFirst(new graphql.language.NodeVisitorStub() {
                @Override public graphql.util.TraversalControl visitField(graphql.language.Field f, graphql.util.TraverserContext<graphql.language.Node> c) {
                    if (f.getAlias() != null) uses.add("alias");
                    if (!f.getDirectives().isEmpty()) uses.add("directive");
                    return graphql.util.TraversalControl.CONTINUE;
                }
                @Override public graphql.util.TraversalControl visitFragmentSpread(graphql.language.FragmentSpread f, graphql.util.TraverserContext<graphql.language.Node> c) {
                    uses.add("fragment-spread");
                    if (!f.getDirectives().isEmpty()) uses.add("directive");
                    return graphql.util.TraversalControl.CONTINUE;
                }
                @Override public graphql.util.TraversalControl visitInlineFragment(graphql.language.InlineFragment f, graphql.util.TraverserContext<graphql.language.Node> c) {
                    uses.add("inline-fragment");
                    if (!f.getDirectives().isEmpty()) uses.add("directive");
                    return graphql.util.TraversalControl.CONTINUE;
                }
            }, op);
            System.out.println(op.getOperation().name().toLowerCase() + " " + (op.getName() == null ? "(anonymous)" : op.getName())
                    + " uses=" + String.join(",", uses));
        }
        return 0;
    }

    static GraphQLSchema schema(String source) throws Exception {
        String sdl;
        if (source.startsWith("http://") || source.startsWith("https://")) {
            var res = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(source)).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) fail("GET " + source + " returned HTTP " + res.statusCode());
            sdl = res.body();
        } else if (Files.isDirectory(Path.of(source))) {
            var registry = new graphql.schema.idl.TypeDefinitionRegistry();
            try (var files = Files.list(Path.of(source))) {
                for (Path f : files.filter(f -> f.toString().endsWith(".graphqls")).sorted().toList()) {
                    registry.merge(new SchemaParser().parse(Files.readString(f)));
                }
            }
            return UnExecutableSchemaGenerator.makeUnExecutableSchema(registry);
        } else {
            sdl = Files.readString(Path.of(source));
        }
        return UnExecutableSchemaGenerator.makeUnExecutableSchema(new SchemaParser().parse(sdl));
    }

    static final java.util.regex.Pattern COORD = java.util.regex.Pattern.compile(
            "(@)?([_A-Za-z][_0-9A-Za-z]*)(?:\\.([_A-Za-z][_0-9A-Za-z]*))?(?:\\(([_A-Za-z][_0-9A-Za-z]*):\\))?");

    /** The kind of element a coordinate names, or null if it names nothing in this schema. */
    static String resolve(GraphQLSchema schema, String coordinate) {
        var m = COORD.matcher(coordinate);
        if (!m.matches()) return null;
        String name = m.group(2), member = m.group(3), arg = m.group(4);
        if (m.group(1) != null) {
            if (member != null) return null;
            GraphQLDirective d = schema.getDirective(name);
            if (d == null) return null;
            if (arg == null) return "directive";
            return d.getArgument(arg) != null ? "directive-argument" : null;
        }
        GraphQLNamedType t = (GraphQLNamedType) schema.getType(name);
        if (t == null || name.startsWith("__")) return null;
        if (member == null) return arg == null ? "type" : null;
        return switch (t) {
            case GraphQLFieldsContainer fc -> {
                GraphQLFieldDefinition f = fc.getFieldDefinition(member);
                if (f == null) yield null;
                if (arg == null) yield "field";
                yield f.getArgument(arg) != null ? "argument" : null;
            }
            case GraphQLInputObjectType in -> arg == null && in.getFieldDefinition(member) != null ? "input-field" : null;
            case GraphQLEnumType e -> arg == null && e.getValue(member) != null ? "enum-value" : null;
            default -> null;
        };
    }

    static int coordinates(GraphQLSchema schema, List<String> coords) {
        int missing = 0;
        for (String c : coords) {
            String kind = resolve(schema, c);
            if (kind == null) { System.out.println("missing " + c); missing++; }
            else System.out.println("ok " + c + " " + kind);
        }
        return missing == 0 ? 0 : 1;
    }

    static String stats(GraphQLSchema schema) {
        int types = 0, fields = 0, args = 0, inputFields = 0, enumValues = 0;
        var builtIn = java.util.Set.of("String", "Int", "Float", "Boolean", "ID");
        for (GraphQLNamedType t : schema.getAllTypesAsList()) {
            if (t.getName().startsWith("__") || builtIn.contains(t.getName())) continue;
            types++;
            if (t instanceof GraphQLFieldsContainer fc) {
                for (GraphQLFieldDefinition f : fc.getFieldDefinitions()) {
                    if (f.isDeprecated()) fields++;
                    for (GraphQLArgument a : f.getArguments()) if (a.isDeprecated()) args++;
                }
            }
            if (t instanceof GraphQLInputObjectType in) for (var f : in.getFieldDefinitions()) if (f.isDeprecated()) inputFields++;
            if (t instanceof GraphQLEnumType e) for (var v : e.getValues()) if (v.isDeprecated()) enumValues++;
        }
        return "{\"types\":" + types + ",\"deprecated\":{\"fields\":" + fields + ",\"arguments\":" + args
                + ",\"inputFields\":" + inputFields + ",\"enumValues\":" + enumValues + "}}";
    }

    static List<String> schemaLines(GraphQLSchema schema) {
        List<String> lines = new ArrayList<>();
        for (GraphQLNamedType t : schema.getAllTypesAsList()) {
            String n = t.getName();
            if (n.startsWith("__")) continue;
            switch (t) {
                case GraphQLObjectType o -> {
                    lines.add("type " + n + interfaces(o.getInterfaces()));
                    o.getFieldDefinitions().forEach(f -> lines.add(n + "." + f.getName() + args(f.getArguments()) + ": " + GraphQLTypeUtil.simplePrint(f.getType())));
                }
                case GraphQLInterfaceType i -> {
                    lines.add("interface " + n + interfaces(i.getInterfaces()));
                    i.getFieldDefinitions().forEach(f -> lines.add(n + "." + f.getName() + args(f.getArguments()) + ": " + GraphQLTypeUtil.simplePrint(f.getType())));
                }
                case GraphQLUnionType u -> lines.add("union " + n + " = " + u.getTypes().stream().map(GraphQLNamedType::getName).sorted().collect(Collectors.joining(" | ")));
                case GraphQLEnumType e -> e.getValues().forEach(v -> lines.add("enum " + n + "." + v.getName()));
                case GraphQLInputObjectType in -> in.getFieldDefinitions().forEach(f -> lines.add("input " + n + "." + f.getName() + ": " + GraphQLTypeUtil.simplePrint(f.getType())));
                case GraphQLScalarType s -> lines.add("scalar " + n);
                default -> lines.add("other " + n);
            }
        }
        lines.add("schema query=" + schema.getQueryType().getName()
                + (schema.getMutationType() == null ? "" : " mutation=" + schema.getMutationType().getName())
                + (schema.getSubscriptionType() == null ? "" : " subscription=" + schema.getSubscriptionType().getName()));
        return lines.stream().sorted().toList();
    }

    static String interfaces(List<? extends GraphQLNamedOutputType> list) {
        return list.isEmpty() ? "" : " implements " + list.stream().map(GraphQLNamedType::getName).sorted().collect(Collectors.joining(" & "));
    }

    static String args(List<GraphQLArgument> list) {
        if (list.isEmpty()) return "";
        return list.stream().map(a -> a.getName() + ": " + GraphQLTypeUtil.simplePrint(a.getType()) + defaultValue(a))
                .collect(Collectors.joining(", ", "(", ")"));
    }

    static String defaultValue(GraphQLArgument a) {
        var d = a.getArgumentDefaultValue();
        if (!d.isSet()) return "";
        Object v = d.getValue();
        return " = " + (v instanceof graphql.language.Node<?> n ? graphql.language.AstPrinter.printAst(n) : String.valueOf(v));
    }

    static void usage() {
        fail("usage: bin/course gql validate <doc.graphql> [--schema <sdl file or url>]\n"
                + "       bin/course gql operations <doc.graphql>\n"
                + "       bin/course gql schema-lines <sdl file, dir or url>\n"
                + "       bin/course gql coordinates <sdl file, dir or url> <coordinate>...\n"
                + "       bin/course gql stats <sdl file, dir or url>");
    }

    static void fail(String msg) {
        System.err.println(msg);
        System.exit(2);
    }
}

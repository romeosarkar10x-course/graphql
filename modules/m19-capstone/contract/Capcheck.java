// The capstone checker's schema and document half (graphql-java 26.1, from the course class path).
//   java Capcheck.java schema <api.graphql>          what the requirement list asks of a schema, as JSON
//   java Capcheck.java ops <api.graphql> <dir>       every operation in dir (fragments may live in other files),
//                                                    validated against the schema, as JSON
//   java Capcheck.java doc <dir> <OperationName>     the operation's full document: it plus the fragments it spreads
//   java Capcheck.java id <manifest.json> <Name>     the persisted id of the document defining that operation
//   java Capcheck.java coords <api.graphql>          every schema coordinate with its type, sorted (to compare schemas)
//   java Capcheck.java printsdl <introspection.json> the SDL of an introspection result (a service without _service)
// <api.graphql> is the schema clients see: the composed API schema for a federated graph.
import graphql.language.*;
import graphql.parser.Parser;
import graphql.schema.*;
import graphql.schema.idl.*;
import graphql.validation.ValidationError;
import graphql.validation.Validator;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

public class Capcheck {
    public static void main(String[] a) throws Exception {
        switch (a.length > 0 ? a[0] : "") {
            case "schema" -> System.out.println(schemaReport(load(Path.of(a[1]))));
            case "ops" -> System.out.println(opsReport(load(Path.of(a[1])), Path.of(a[2])));
            case "doc" -> System.out.print(document(Path.of(a[1]), a[2]));
            case "id" -> System.out.println(persistedId(Path.of(a[1]), a[2]));
            case "coords" -> coords(load(Path.of(a[1]))).forEach(System.out::println);
            case "printsdl" -> System.out.println(printFromIntrospection(Path.of(a[1])));
            default -> { System.err.println("usage: Capcheck schema|ops|doc|id ..."); System.exit(2); }
        }
    }

    static GraphQLSchema load(Path sdl) throws Exception {
        // the API schema may declare @oneOf itself; graphql-java has it built in
        String text = Files.readString(sdl).replaceAll("(?m)^directive @oneOf[^\\n]*\\n", "");
        TypeDefinitionRegistry reg = new SchemaParser().parse(text);
        return new SchemaGenerator().makeExecutableSchema(reg, RuntimeWiring.newRuntimeWiring().wiringFactory(new MockedWiringFactory()).build());
    }

    static boolean builtIn(String name) {
        return name.startsWith("__") || name.startsWith("_") || name.startsWith("link__") || name.startsWith("join__")
                || Set.of("String", "Int", "Float", "Boolean", "ID").contains(name);
    }

    static GraphQLNamedType named(GraphQLType t) {
        return (GraphQLNamedType) GraphQLTypeUtil.unwrapAll(t);
    }

    static String schemaReport(GraphQLSchema s) {
        Set<String> roots = new HashSet<>();
        for (GraphQLObjectType r : new GraphQLObjectType[] {s.getQueryType(), s.getMutationType(), s.getSubscriptionType()})
            if (r != null) roots.add(r.getName());
        List<GraphQLNamedType> types = s.getAllTypesAsList().stream()
                .filter(t -> !builtIn(t.getName()) && !roots.contains(t.getName())).toList();
        Map<String, Integer> interfaces = new TreeMap<>();
        for (GraphQLNamedType t : types)
            if (t instanceof GraphQLInterfaceType i) interfaces.put(i.getName(), s.getImplementations(i).size());
        List<String> unions = types.stream().filter(t -> t instanceof GraphQLUnionType).map(GraphQLNamedType::getName).toList();
        List<String> oneOf = types.stream().filter(t -> t instanceof GraphQLInputObjectType i && i.isOneOf()).map(GraphQLNamedType::getName).toList();

        // connections: <X>Connection with edges and pageInfo, returned by a field with first/after and filter and sort arguments
        List<String> connections = new ArrayList<>();
        List<String> pagedFields = new ArrayList<>();
        List<String> filteredSortedFields = new ArrayList<>();
        for (GraphQLNamedType t : types) {
            if (!(t instanceof GraphQLObjectType o) || !o.getName().endsWith("Connection")) continue;
            if (o.getFieldDefinition("edges") == null || o.getFieldDefinition("pageInfo") == null) continue;
            connections.add(o.getName());
        }
        for (GraphQLNamedType t : s.getAllTypesAsList()) {
            if (!(t instanceof GraphQLFieldsContainer c) || builtIn(t.getName())) continue;
            for (GraphQLFieldDefinition f : c.getFieldDefinitions()) {
                if (!connections.contains(named(f.getType()).getName())) continue;
                Set<String> args = f.getArguments().stream().map(GraphQLArgument::getName).collect(Collectors.toSet());
                if (!(args.contains("first") && args.contains("after"))) continue;
                String coord = c.getName() + "." + f.getName();
                pagedFields.add(coord);
                boolean filter = f.getArguments().stream().anyMatch(arg -> named(arg.getType()) instanceof GraphQLInputObjectType
                        && !arg.getName().matches("(?i)sort|order(By)?"));
                boolean sort = f.getArguments().stream().anyMatch(arg -> arg.getName().matches("(?i)sort(By)?|order(By)?")
                        || (named(arg.getType()) instanceof GraphQLEnumType && arg.getName().toLowerCase().contains("sort")));
                if (filter && sort) filteredSortedFields.add(coord);
            }
        }
        // mutations whose result can carry an expected failure as data: a union, an interface, or a payload with an
        // errors-like field of a union, interface or object type with a message
        List<String> typedErrorMutations = new ArrayList<>();
        if (s.getMutationType() != null) {
            for (GraphQLFieldDefinition f : s.getMutationType().getFieldDefinitions()) {
                GraphQLNamedType r = named(f.getType());
                boolean typed = r instanceof GraphQLUnionType || r instanceof GraphQLInterfaceType;
                if (r instanceof GraphQLObjectType o)
                    typed = o.getFieldDefinitions().stream().anyMatch(pf -> {
                        GraphQLNamedType pt = named(pf.getType());
                        return pf.getName().matches("(?i).*(error|problem)s?") && (pt instanceof GraphQLUnionType || pt instanceof GraphQLInterfaceType
                                || pt instanceof GraphQLObjectType po && po.getFieldDefinition("message") != null);
                    });
                if (typed) typedErrorMutations.add("Mutation." + f.getName());
            }
        }
        List<String> subscriptions = s.getSubscriptionType() == null ? List.of()
                : s.getSubscriptionType().getFieldDefinitions().stream().map(f -> "Subscription." + f.getName()).toList();
        boolean deferDirective = s.getDirective("defer") != null;
        long described = types.stream().filter(t -> t.getDescription() != null && !t.getDescription().isBlank()).count();
        return "{" + String.join(",",
                kv("types", types.size()), kv("typeNames", types.stream().map(GraphQLNamedType::getName).sorted().toList()),
                kv("interfaces", interfaces), kv("unions", unions), kv("oneOf", oneOf), kv("connections", connections),
                kv("pagedFields", pagedFields), kv("filteredSortedFields", filteredSortedFields),
                kv("typedErrorMutations", typedErrorMutations), kv("subscriptions", subscriptions),
                kv("deferDirective", deferDirective), kv("describedTypes", described)) + "}";
    }

    @SuppressWarnings("unchecked")
    static String printFromIntrospection(Path json) throws Exception {
        Map<String, Object> result = new tools.jackson.databind.json.JsonMapper().readValue(Files.readString(json), Map.class);
        Map<String, Object> data = (Map<String, Object>) result.getOrDefault("data", result);
        Document doc = new graphql.introspection.IntrospectionResultToSchema().createSchemaDefinition(data);
        return AstPrinter.printAst(doc);
    }

    static List<String> coords(GraphQLSchema s) {
        List<String> out = new ArrayList<>();
        for (GraphQLNamedType t : s.getAllTypesAsList()) {
            if (builtIn(t.getName())) continue;
            switch (t) {
                case GraphQLFieldsContainer c -> c.getFieldDefinitions().forEach(f -> out.add(c.getName() + "." + f.getName()
                        + f.getArguments().stream().map(x -> x.getName() + ": " + GraphQLTypeUtil.simplePrint(x.getType())).sorted()
                                .collect(Collectors.joining(", ", "(", ")")) + ": " + GraphQLTypeUtil.simplePrint(f.getType())));
                case GraphQLInputObjectType i -> i.getFieldDefinitions().forEach(f -> out.add(i.getName() + "." + f.getName() + ": " + GraphQLTypeUtil.simplePrint(f.getType())));
                case GraphQLEnumType e -> e.getValues().forEach(v -> out.add(e.getName() + "." + v.getName()));
                case GraphQLUnionType u -> out.add(u.getName() + " = " + u.getTypes().stream().map(GraphQLNamedType::getName).sorted().collect(Collectors.joining(" | ")));
                default -> out.add(t.getName());
            }
        }
        Collections.sort(out);
        return out;
    }

    record Op(String name, String kind, String file, OperationDefinition def) {}

    static Map<String, FragmentDefinition> fragments = new HashMap<>();
    static Map<String, String> fragmentFile = new HashMap<>();
    static List<Op> ops = new ArrayList<>();
    static List<String> parseErrors = new ArrayList<>();

    static void scan(Path dir) throws Exception {
        List<Path> files;
        try (Stream<Path> w = Files.walk(dir)) {
            files = w.filter(p -> p.toString().endsWith(".graphql") || p.toString().endsWith(".gql")).sorted().toList();
        }
        for (Path f : files) {
            Document d;
            try {
                d = Parser.parse(Files.readString(f));
            } catch (Exception e) {
                parseErrors.add(dir.relativize(f) + ": " + e.getMessage().lines().findFirst().orElse(""));
                continue;
            }
            for (Definition<?> def : d.getDefinitions()) {
                if (def instanceof FragmentDefinition fd) { fragments.put(fd.getName(), fd); fragmentFile.put(fd.getName(), dir.relativize(f).toString()); }
                if (def instanceof OperationDefinition od)
                    ops.add(new Op(od.getName(), od.getOperation().name().toLowerCase(), dir.relativize(f).toString(), od));
            }
        }
    }

    static Set<String> spreads(Node<?> n, Set<String> acc) {
        if (n instanceof FragmentSpread fs && acc.add(fs.getName()) && fragments.containsKey(fs.getName()))
            spreads(fragments.get(fs.getName()), acc);
        for (Node<?> c : n.getChildren()) spreads(c, acc);
        return acc;
    }

    static boolean usesDefer(Node<?> n) {
        if (n instanceof DirectivesContainer<?> dc && dc.getDirectives().stream().anyMatch(d -> d.getName().equals("defer"))) return true;
        for (Node<?> c : n.getChildren()) if (usesDefer(c)) return true;
        return false;
    }

    static Document documentOf(Op op) {
        List<Definition> defs = new ArrayList<>(List.of(op.def()));
        for (String f : new TreeSet<>(spreads(op.def(), new HashSet<>()))) if (fragments.containsKey(f)) defs.add(fragments.get(f));
        return Document.newDocument().definitions(defs).build();
    }

    static String opsReport(GraphQLSchema s, Path dir) throws Exception {
        scan(dir);
        List<String> rows = new ArrayList<>();
        for (Op op : ops) {
            Set<String> missing = spreads(op.def(), new HashSet<>()).stream().filter(f -> !fragments.containsKey(f)).collect(Collectors.toSet());
            List<String> errors = new ArrayList<>(missing.stream().map(f -> "fragment " + f + " is not defined in " + dir).toList());
            if (missing.isEmpty())
                for (ValidationError e : new Validator().validateDocument(s, documentOf(op), Locale.ENGLISH)) errors.add(e.getMessage());
            rows.add("{" + String.join(",", kv("name", op.name()), kv("kind", op.kind()), kv("file", op.file()),
                    kv("defer", usesDefer(op.def())), kv("errors", errors)) + "}");
        }
        return "{" + kv("parseErrors", parseErrors) + ",\"operations\":[" + String.join(",", rows) + "]}";
    }

    static String document(Path dir, String name) throws Exception {
        scan(dir);
        Op op = ops.stream().filter(o -> name.equals(o.name())).findFirst().orElseThrow(() -> new IllegalArgumentException("no operation " + name + " in " + dir));
        return AstPrinter.printAst(documentOf(op));
    }

    static String persistedId(Path manifest, String name) throws Exception {
        // {"<id>": "<document>"} (Hive, Relay, GraphQL Codegen) or Apollo's {"operations": [{"id", "name", "body"}]}
        String json = Files.readString(manifest);
        Pattern op = Pattern.compile("(query|mutation|subscription)\\s+" + Pattern.quote(name) + "\\b");
        Matcher apollo = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"[^}]*?\"body\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        while (apollo.find()) if (op.matcher(unescape(apollo.group(2))).find()) return apollo.group(1);
        Matcher kv = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        while (kv.find()) if (op.matcher(unescape(kv.group(2))).find()) return kv.group(1);
        throw new IllegalArgumentException("no document for operation " + name + " in " + manifest);
    }

    static String unescape(String s) {
        return s.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    static String kv(String k, Object v) {
        return "\"" + k + "\":" + json(v);
    }

    static String json(Object v) {
        if (v instanceof String s) return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\"";
        if (v instanceof Collection<?> c) return "[" + c.stream().map(Capcheck::json).collect(Collectors.joining(",")) + "]";
        if (v instanceof Map<?, ?> m) return "{" + m.entrySet().stream().map(e -> kv(e.getKey().toString(), e.getValue())).collect(Collectors.joining(",")) + "}";
        return String.valueOf(v);
    }
}

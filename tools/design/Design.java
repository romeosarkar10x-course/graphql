/*
 * Module 7 schema-design helper on graphql-java 26.1. Run it as `bin/course design <command> ...`.
 *
 *   lint <schema> [--rules r1,r2,...]
 *       Design lint. Prints "fail <rule>: <where>" per problem, then "ok <rule>" for clean rules.
 *       Rules: naming, descriptions, references, json-scalars, boolean-args, kitchen-sink, db-names,
 *       crud-names, timestamps. Default: naming,descriptions,references.
 *   shape <schema> <query.graphql> <view-model.json>
 *       Runs the query against mock data and compares the response's shape (its key paths, lists as
 *       []) with the screen's view model. Prints missing paths (the screen needs them) and extra paths
 *       (the query fetches what the screen does not show; `id` fields never count as extra, client
 *       caches want them). Aliases are how a query matches a screen's key names. Exit 1 unless they match.
 *   clients <schema> <file.graphql | dir>...
 *       Validates every client document against the schema: "ok <file>" or "broken <file>: <why>".
 *   diff <old schema> <new schema>
 *       graphql-java SchemaDiff, one line per BREAKING or DANGEROUS event, then
 *       "verdict: BREAKING | DANGEROUS | SAFE". Exit 0 whatever the verdict.
 *   outage <schema> <query.graphql> [--fail Type.field,...] [--no-propagation]
 *       Runs the query against mock data with the listed fields throwing (a dependency is down).
 *       Prints the data and "survived <n> of <m> leaves". --no-propagation adds graphql-java's
 *       @experimental_disableErrorPropagation to the operation: errors stay where they happen.
 *
 * Mock data: String "Type.field", Int 1, Float 1.5, Boolean true, ID "Type:1", enums their first value,
 * lists two items (an abstract list one item per possible type, in name order), abstract types
 * resolved in name order. A <schema> that is a directory means every *.graphqls file in it, merged.
 * Exit status: 0 ok, 1 problems found, 2 usage or I/O problem.
 */
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.ParseAndValidate;
import graphql.language.Document;
import graphql.parser.Parser;
import graphql.schema.*;
import graphql.schema.diff.DiffLevel;
import graphql.schema.diff.SchemaDiff;
import graphql.schema.diff.SchemaDiffSet;
import graphql.schema.diff.reporting.CapturingReporter;
import graphql.schema.idl.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class Design {
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final Set<String> BUILTIN_SCALARS = Set.of("String", "Int", "Float", "Boolean", "ID");

    public static void main(String[] a) throws Exception {
        if (a.length == 0) usage();
        int code = switch (a[0]) {
            case "lint" -> lint(a);
            case "shape" -> { need(a, 4); yield shape(a[1], Files.readString(Path.of(a[2])), Path.of(a[3])); }
            case "clients" -> { need(a, 3); yield clients(a); }
            case "diff" -> { need(a, 3); yield diff(sdl(a[1]), sdl(a[2])); }
            case "outage" -> outage(a);
            default -> { usage(); yield 2; }
        };
        System.exit(code);
    }

    static void usage() {
        System.err.println("usage: design lint|shape|clients|diff|outage ...   (see the comment at the top of tools/design/Design.java)");
        System.exit(2);
    }

    static void need(String[] a, int n) { if (a.length < n) usage(); }

    // ---------------------------------------------------------------- loading
    static String sdl(String path) throws Exception {
        Path p = Path.of(path);
        if (!Files.exists(p)) { System.err.println("design: no such file " + path); System.exit(2); }
        if (!Files.isDirectory(p)) return Files.readString(p);
        try (Stream<Path> s = Files.list(p)) {
            var sb = new StringBuilder();
            for (Path f : s.filter(f -> f.toString().endsWith(".graphqls")).sorted().toList()) sb.append(Files.readString(f)).append('\n');
            return sb.toString();
        }
    }

    static TypeDefinitionRegistry registry(String sdl) { return new SchemaParser().parse(sdl); }

    static GraphQLSchema schema(String path) throws Exception {
        return UnExecutableSchemaGenerator.makeUnExecutableSchema(registry(sdl(path)));
    }

    static List<GraphQLNamedType> userTypes(GraphQLSchema s) {
        return s.getAllTypesAsList().stream()
            .filter(t -> !t.getName().startsWith("__") && !BUILTIN_SCALARS.contains(t.getName()))
            .sorted(Comparator.comparing(GraphQLNamedType::getName)).toList();
    }

    // ---------------------------------------------------------------- lint
    record Problem(String rule, String where) {}

    static int lint(String[] a) throws Exception {
        need(a, 2);
        List<String> rules = List.of("naming", "descriptions", "references");
        for (int i = 2; i + 1 < a.length; i++) if (a[i].equals("--rules")) rules = List.of(a[i + 1].split(","));
        GraphQLSchema s = schema(a[1]);
        List<Problem> out = new ArrayList<>();
        for (String r : rules) {
            switch (r) {
                case "naming" -> naming(s, out);
                case "descriptions" -> descriptions(s, out);
                case "references" -> references(s, out);
                case "json-scalars" -> jsonScalars(s, out);
                case "boolean-args" -> booleanArgs(s, out);
                case "kitchen-sink" -> kitchenSink(s, out);
                case "db-names" -> dbNames(s, out);
                case "crud-names" -> crudNames(s, out);
                case "timestamps" -> timestamps(s, out);
                default -> { System.err.println("design: unknown rule " + r); return 2; }
            }
        }
        out.forEach(p -> System.out.println("fail " + p.rule() + ": " + p.where()));
        Set<String> failed = out.stream().map(Problem::rule).collect(Collectors.toSet());
        rules.stream().filter(r -> !failed.contains(r)).forEach(r -> System.out.println("ok " + r));
        return out.isEmpty() ? 0 : 1;
    }

    static boolean root(GraphQLSchema s, GraphQLNamedType t) {
        return Stream.of(s.getQueryType(), s.getMutationType(), s.getSubscriptionType()).anyMatch(r -> r != null && r.getName().equals(t.getName()));
    }

    /** Every (coordinate, field) of object and interface types. */
    static List<Map.Entry<String, GraphQLFieldDefinition>> outputFields(GraphQLSchema s) {
        List<Map.Entry<String, GraphQLFieldDefinition>> l = new ArrayList<>();
        for (GraphQLNamedType t : userTypes(s))
            if (t instanceof GraphQLFieldsContainer c)
                for (GraphQLFieldDefinition f : c.getFieldDefinitions()) l.add(Map.entry(t.getName() + "." + f.getName(), f));
        return l;
    }

    static void naming(GraphQLSchema s, List<Problem> out) {
        for (GraphQLNamedType t : userTypes(s)) {
            if (!t.getName().matches("[A-Z][A-Za-z0-9]*")) out.add(new Problem("naming", "type " + t.getName() + " (PascalCase)"));
            if (t instanceof GraphQLEnumType e)
                for (var v : e.getValues()) if (!v.getName().matches("[A-Z][A-Z0-9_]*")) out.add(new Problem("naming", t.getName() + "." + v.getName() + " (UPPER_CASE enum values)"));
            if (t instanceof GraphQLInputObjectType in)
                for (var f : in.getFieldDefinitions()) if (!f.getName().matches("[a-z][A-Za-z0-9]*")) out.add(new Problem("naming", t.getName() + "." + f.getName() + " (camelCase)"));
        }
        for (var e : outputFields(s)) {
            if (!e.getValue().getName().matches("[a-z][A-Za-z0-9]*")) out.add(new Problem("naming", e.getKey() + " (camelCase fields)"));
            for (var arg : e.getValue().getArguments())
                if (!arg.getName().matches("[a-z][A-Za-z0-9]*")) out.add(new Problem("naming", e.getKey() + "(" + arg.getName() + ":) (camelCase arguments)"));
        }
    }

    static boolean blank(String d) { return d == null || d.isBlank(); }

    static void descriptions(GraphQLSchema s, List<Problem> out) {
        for (GraphQLNamedType t : userTypes(s)) {
            if (!root(s, t) && blank(t.getDescription())) out.add(new Problem("descriptions", "type " + t.getName()));
            if (t instanceof GraphQLFieldsContainer c)
                for (var f : c.getFieldDefinitions()) if (blank(f.getDescription())) out.add(new Problem("descriptions", t.getName() + "." + f.getName()));
            if (t instanceof GraphQLEnumType e)
                for (var v : e.getValues()) if (blank(v.getDescription())) out.add(new Problem("descriptions", t.getName() + "." + v.getName()));
        }
    }

    /** Output fields point at objects, not at keys: author, not authorId. Inputs may carry ids, and so
     *  may deleted*Id payload fields: a deleted object cannot be returned. */
    static void references(GraphQLSchema s, List<Problem> out) {
        for (var e : outputFields(s)) {
            String n = e.getValue().getName();
            if (n.startsWith("deleted")) continue;
            if (n.matches("[a-z][A-Za-z0-9]*(Id|Ids|ID|IDs)") || n.matches("[a-z0-9_]+_ids?")) out.add(new Problem("references", e.getKey() + " (return the object, not its key)"));
        }
    }

    static void jsonScalars(GraphQLSchema s, List<Problem> out) {
        for (GraphQLNamedType t : userTypes(s))
            if (t instanceof GraphQLScalarType && t.getName().matches("(?i)json|any|object|map|data|blob|anything"))
                out.add(new Problem("json-scalars", "scalar " + t.getName() + " (an untyped escape hatch: model the shape)"));
    }

    static void booleanArgs(GraphQLSchema s, List<Problem> out) {
        for (var e : outputFields(s))
            for (var arg : e.getValue().getArguments())
                if (GraphQLTypeUtil.unwrapAll(arg.getType()).getName().equals("Boolean"))
                    out.add(new Problem("boolean-args", e.getKey() + "(" + arg.getName() + ":) (a flag argument: use an enum or a separate field)"));
    }

    /** One update mutation that edits everything: more than 5 input fields besides the id. */
    static void kitchenSink(GraphQLSchema s, List<Problem> out) {
        if (s.getMutationType() == null) return;
        for (var f : s.getMutationType().getFieldDefinitions()) {
            int fields = 0;
            for (var arg : f.getArguments()) {
                if (GraphQLTypeUtil.unwrapAll(arg.getType()) instanceof GraphQLInputObjectType in)
                    fields += (int) in.getFieldDefinitions().stream().filter(x -> !x.getName().matches("(?i)id|[a-z]+Id")).count();
                else if (!arg.getName().matches("(?i)id|[a-z]+Id")) fields++;
            }
            if (f.getName().startsWith("update") && fields > 5)
                out.add(new Problem("kitchen-sink", "Mutation." + f.getName() + " takes " + fields + " fields (split it by intent)"));
        }
    }

    static void dbNames(GraphQLSchema s, List<Problem> out) {
        for (GraphQLNamedType t : userTypes(s)) {
            if (t.getName().matches(".*(Dto|DTO|Entity|Row|Record|Table|Tbl)$") || t.getName().matches("(?i)^tbl.*"))
                out.add(new Problem("db-names", "type " + t.getName() + " (a storage or transport name)"));
            if (t instanceof GraphQLFieldsContainer c)
                for (var f : c.getFieldDefinitions())
                    if (f.getName().contains("_") || f.getName().matches("(?i).*(fk|pk|rowid|tbl).*"))
                        out.add(new Problem("db-names", t.getName() + "." + f.getName() + " (a column name)"));
        }
    }

    static void crudNames(GraphQLSchema s, List<Problem> out) {
        if (s.getQueryType() != null)
            for (var f : s.getQueryType().getFieldDefinitions())
                if (f.getName().matches("(get|select|find|fetch|list|all|read)[A-Z].*") || f.getName().matches("(?i).*(raw|sql|table).*"))
                    out.add(new Problem("crud-names", "Query." + f.getName() + " (name the thing, not the operation)"));
        if (s.getMutationType() != null)
            for (var f : s.getMutationType().getFieldDefinitions())
                if (f.getName().matches("(insert|upsert|save|put|patch|crud|write|delete)[A-Z].*Row.*|(insert|upsert|save|put|patch|crud|write)[A-Z].*")
                        || f.getName().matches("(?i).*(raw|sql|table).*"))
                    out.add(new Problem("crud-names", "Mutation." + f.getName() + " (name the intent, not the table operation)"));
    }

    static void timestamps(GraphQLSchema s, List<Problem> out) {
        for (var e : outputFields(s)) {
            var f = e.getValue();
            String type = GraphQLTypeUtil.unwrapAll(f.getType()).getName();
            if ((f.getName().matches("[a-z][A-Za-z]*(At|On|Date|Time|Ts)") || f.getName().matches("[a-z0-9_]+_(at|on|date|time|ts)"))
                    && Set.of("Int", "Float", "String").contains(type))
                out.add(new Problem("timestamps", e.getKey() + ": " + type + " (use a DateTime or Date scalar)"));
        }
    }

    // ---------------------------------------------------------------- mock execution
    static GraphQL mock(String sdl, Set<String> failing) {
        TypeDefinitionRegistry reg = registry(sdl);
        WiringFactory wf = new WiringFactory() {
            @Override public boolean providesScalar(ScalarWiringEnvironment env) { return !ScalarInfo.isGraphqlSpecifiedScalar(env.getScalarTypeDefinition().getName()); }
            @Override public GraphQLScalarType getScalar(ScalarWiringEnvironment env) {
                return GraphQLScalarType.newScalar().name(env.getScalarTypeDefinition().getName()).coercing(new Coercing<Object, Object>() {
                    @Override public Object serialize(Object o, graphql.GraphQLContext c, Locale l) { return o; }
                    @Override public Object parseValue(Object o, graphql.GraphQLContext c, Locale l) { return o; }
                    @Override public Object parseLiteral(graphql.language.Value<?> v, graphql.execution.CoercedVariables cv, graphql.GraphQLContext c, Locale l) { return v; }
                }).build();
            }
            @Override public boolean providesTypeResolver(InterfaceWiringEnvironment env) { return true; }
            @Override public TypeResolver getTypeResolver(InterfaceWiringEnvironment env) { return Design::resolve; }
            @Override public boolean providesTypeResolver(UnionWiringEnvironment env) { return true; }
            @Override public TypeResolver getTypeResolver(UnionWiringEnvironment env) { return Design::resolve; }
            @Override public DataFetcher<?> getDefaultDataFetcher(FieldWiringEnvironment env) {
                return e -> {
                    String parent = ((GraphQLNamedType) e.getParentType()).getName(), coord = parent + "." + e.getField().getName();
                    if (failing.contains(coord)) throw new IllegalStateException(coord + " is down");
                    return value(e.getGraphQLSchema(), e.getFieldType(), coord, 0);
                };
            }
        };
        var schema = new SchemaGenerator().makeExecutableSchema(reg, RuntimeWiring.newRuntimeWiring().wiringFactory(wf).build());
        return GraphQL.newGraphQL(schema).build();
    }

    static GraphQLObjectType resolve(graphql.TypeResolutionEnvironment env) {
        Object typename = ((Map<?, ?>) env.getObject()).get("__typename");
        return env.getSchema().getObjectType((String) typename);
    }

    static Object value(GraphQLSchema s, GraphQLType type, String coord, int index) {
        type = GraphQLTypeUtil.unwrapNonNull(type);
        if (type instanceof GraphQLList l) {
            GraphQLType item = GraphQLTypeUtil.unwrapNonNull(l.getWrappedType());
            int n = item instanceof GraphQLNamedType nt && possible(s, nt).size() > 1 ? possible(s, nt).size() : 2;
            List<Object> out = new ArrayList<>();
            for (int i = 0; i < n; i++) out.add(value(s, l.getWrappedType(), coord, i));
            return out;
        }
        String name = ((GraphQLNamedType) type).getName();
        return switch (type) {
            case GraphQLEnumType e -> e.getValues().getFirst().getName();
            case GraphQLObjectType o -> new HashMap<>(Map.of("__typename", o.getName()));
            case GraphQLInterfaceType i -> new HashMap<>(Map.of("__typename", possible(s, i).get(index % possible(s, i).size())));
            case GraphQLUnionType u -> new HashMap<>(Map.of("__typename", possible(s, u).get(index % possible(s, u).size())));
            default -> switch (name) {
                case "Int" -> 1; case "Float" -> 1.5; case "Boolean" -> true;
                case "ID" -> coord.substring(0, coord.indexOf('.')) + ":" + (index + 1);
                default -> coord;
            };
        };
    }

    static List<String> possible(GraphQLSchema s, GraphQLNamedType t) {
        Stream<GraphQLNamedType> types = switch (t) {
            case GraphQLInterfaceType i -> s.getImplementations(i).stream().map(o -> o);
            case GraphQLUnionType u -> u.getTypes().stream().map(o -> o);
            default -> Stream.of(t);
        };
        return types.map(GraphQLNamedType::getName).sorted().toList();
    }

    /** Executes with mock values for every variable the operation declares. */
    static Map<String, Object> run(GraphQL g, String query) {
        Map<String, Object> vars = new HashMap<>();
        try {
            for (var op : Parser.parse(query).getDefinitionsOfType(graphql.language.OperationDefinition.class))
                for (var v : op.getVariableDefinitions()) vars.putIfAbsent(v.getName(), input(g.getGraphQLSchema(), v.getType()));
        } catch (Exception ignored) { /* execution reports it */ }
        return g.execute(ExecutionInput.newExecutionInput(query).variables(vars).build()).toSpecification();
    }

    static Object input(GraphQLSchema s, graphql.language.Type<?> t) {
        if (t instanceof graphql.language.NonNullType nn) return input(s, nn.getType());
        if (t instanceof graphql.language.ListType l) return List.of(input(s, l.getType()));
        return input(s, s.getType(((graphql.language.TypeName) t).getName()));
    }

    static Object input(GraphQLSchema s, GraphQLType t) {
        t = GraphQLTypeUtil.unwrapNonNull(t);
        if (t instanceof GraphQLList l) return List.of(input(s, l.getWrappedType()));
        return switch (t) {
            case GraphQLEnumType e -> e.getValues().getFirst().getName();
            case GraphQLInputObjectType in -> {
                Map<String, Object> m = new LinkedHashMap<>();
                for (var f : in.getFieldDefinitions()) if (GraphQLTypeUtil.isNonNull(f.getType()) && !f.hasSetDefaultValue()) m.put(f.getName(), input(s, f.getType()));
                if (m.isEmpty() && !in.getFieldDefinitions().isEmpty()) m.put(in.getFieldDefinitions().getFirst().getName(), input(s, in.getFieldDefinitions().getFirst().getType()));
                yield m;
            }
            default -> switch (((GraphQLNamedType) t).getName()) { case "Int" -> 1; case "Float" -> 1.5; case "Boolean" -> true; default -> "x"; };
        };
    }

    // ---------------------------------------------------------------- shape
    static int shape(String schemaPath, String query, Path viewModel) throws Exception {
        String sdl = sdl(schemaPath);
        var check = ParseAndValidate.parseAndValidate(UnExecutableSchemaGenerator.makeUnExecutableSchema(registry(sdl)),
            ExecutionInput.newExecutionInput(query).build());
        if (check.isFailure()) {
            check.getErrors().forEach(e -> System.out.println("invalid: " + e.getMessage()));
            return 1;
        }
        Map<String, Object> result = run(mock(sdl, Set.of()), query);
        if (result.containsKey("errors")) { System.out.println("errors: " + result.get("errors")); return 1; }
        Set<String> got = new TreeSet<>(), want = new TreeSet<>();
        paths(JSON.valueToTree(result.get("data")), "", got);
        paths(JSON.readTree(Files.readString(viewModel)), "", want);
        Set<String> missing = new TreeSet<>(want), extra = new TreeSet<>(got);
        missing.removeAll(got);
        extra.removeAll(want);
        extra.removeIf(p -> p.equals("id") || p.endsWith(".id"));   // ids for client caches are always welcome
        missing.forEach(p -> System.out.println("missing " + p + "   (the screen shows it; the query does not fetch it)"));
        extra.forEach(p -> System.out.println("extra   " + p + "   (the query fetches it; the screen does not show it)"));
        if (missing.isEmpty() && extra.isEmpty()) System.out.println("ok shape: " + want.size() + " paths match " + viewModel.getFileName());
        return missing.isEmpty() && extra.isEmpty() ? 0 : 1;
    }

    /** Leaf paths: a.b, a[].c. Objects in a list merge, so a feed with three kinds lists every kind's keys. */
    static void paths(JsonNode n, String prefix, Set<String> out) {
        if (n.isObject()) {
            for (var e : n.properties()) {
                if (e.getKey().equals("__typename")) continue;
                paths(e.getValue(), prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey(), out);
            }
        } else if (n.isArray()) {
            if (n.isEmpty()) out.add(prefix + "[]");
            for (JsonNode item : n) paths(item, prefix + "[]", out);
        } else if (!prefix.isEmpty()) out.add(prefix);
    }

    // ---------------------------------------------------------------- clients
    static int clients(String[] a) throws Exception {
        GraphQLSchema s = schema(a[1]);
        List<Path> docs = new ArrayList<>();
        for (int i = 2; i < a.length; i++) {
            Path p = Path.of(a[i]);
            if (Files.isDirectory(p)) try (Stream<Path> st = Files.list(p)) { st.filter(f -> f.toString().endsWith(".graphql")).sorted().forEach(docs::add); }
            else docs.add(p);
        }
        int broken = 0;
        for (Path d : docs) {
            String text = Files.readString(d);
            List<String> why = new ArrayList<>();
            Document doc;
            try { doc = Parser.parse(text); }
            catch (Exception e) { System.out.println("broken " + d + ": does not parse: " + e.getMessage()); broken++; continue; }
            for (var op : doc.getDefinitionsOfType(graphql.language.OperationDefinition.class)) {
                var in = ExecutionInput.newExecutionInput(text).operationName(op.getName()).build();
                var r = ParseAndValidate.parseAndValidate(s, in);
                if (r.isFailure()) r.getErrors().forEach(e -> why.add(e.getMessage()));
            }
            if (why.isEmpty()) System.out.println("ok " + d);
            else { System.out.println("broken " + d + ": " + String.join(" | ", new LinkedHashSet<>(why))); broken++; }
        }
        System.out.println(broken == 0 ? "all " + docs.size() + " clients ok" : broken + " of " + docs.size() + " clients broken");
        return broken == 0 ? 0 : 1;
    }

    static int diff(String oldSdl, String newSdl) {
        var r = new CapturingReporter();
        new SchemaDiff().diffSchema(SchemaDiffSet.diffSetFromSdl(oldSdl, newSdl), r);
        r.getBreakages().forEach(e -> System.out.println("BREAKING  " + e.getReasonMsg()));
        r.getDangers().forEach(e -> System.out.println("DANGEROUS " + e.getReasonMsg()));
        System.out.println("verdict: " + (r.getBreakageCount() > 0 ? "BREAKING" : r.getDangerCount() > 0 ? "DANGEROUS" : "SAFE"));
        return 0;
    }

    static int outage(String[] a) throws Exception {
        need(a, 3);
        Set<String> failing = new LinkedHashSet<>();
        boolean noPropagation = false;
        for (int i = 3; i < a.length; i++) {
            if (a[i].equals("--fail") && i + 1 < a.length) failing.addAll(List.of(a[++i].split(",")));
            else if (a[i].equals("--no-propagation")) noPropagation = true;
        }
        String sdl = sdl(a[1]), query = Files.readString(Path.of(a[2]));
        if (noPropagation) {
            Document doc = Parser.parse(query);
            var defs = doc.getDefinitions().stream().map(d -> d instanceof graphql.language.OperationDefinition op
                ? op.transform(b -> b.directives(Stream.concat(op.getDirectives().stream(),
                    Stream.of(graphql.language.Directive.newDirective().name("experimental_disableErrorPropagation").build())).toList()))
                : d).toList();
            query = graphql.language.AstPrinter.printAst(doc.transform(b -> b.definitions(defs)));
        }
        Map<String, Object> healthy = run(mock(sdl, Set.of()), query);
        if (healthy.containsKey("errors")) { System.out.println("errors with every dependency up: " + healthy.get("errors")); return 1; }
        Map<String, Object> r = run(mock(sdl, failing), query);
        int all = leaves(JSON.valueToTree(healthy.get("data"))), left = leaves(JSON.valueToTree(r.get("data")));
        System.out.println(JSON.writerWithDefaultPrettyPrinter().writeValueAsString(r.get("data")));
        Object errors = r.get("errors");
        System.out.println("errors: " + (errors == null ? 0 : ((List<?>) errors).size()));
        System.out.println("survived " + left + " of " + all + " leaves");
        return 0;
    }

    static int leaves(JsonNode n) {
        if (n == null || n.isNull()) return 0;
        if (n.isObject()) { int c = 0; for (var e : n.properties()) if (!e.getKey().equals("__typename")) c += leaves(e.getValue()); return c; }
        if (n.isArray()) { int c = 0; for (JsonNode i : n) c += leaves(i); return c; }
        return 1;
    }
}

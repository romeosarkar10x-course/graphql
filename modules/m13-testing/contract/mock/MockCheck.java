import graphql.language.Argument;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.IntValue;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.StringValue;
import graphql.language.VariableReference;
import graphql.parser.Parser;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLUnionType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.MockedWiringFactory;
import graphql.validation.ValidationError;
import graphql.validation.Validator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Checks what a mock server returns, without knowing anything about how it makes data up (Module 13.4 and
 * mini-project 13.10). For every query file it POSTs the document twice and checks the response against the
 * schema and the document:
 *
 *   shape        data has exactly the fields the document selects (fragments applied by __typename), every
 *                value has its field's type, non-null is never null, enums are declared values, abstract
 *                types come back as one of their possible types, there are no errors
 *   determinism  the second response is identical to the first
 *   realism      (with --realistic) emails contain @, urls start with http, *At and *Date fields parse as
 *                ISO 8601, *Count is never negative, ids are unique within a list and echo an id argument,
 *                first/last/limit arguments give lists of exactly that length (else 1 to 5 items),
 *                strings are not placeholders such as "String" or the field's own name, and no String or
 *                Int field gives the same value at every position of a list
 *
 * Usage: MockCheck <graphql-url> <schema.graphqls> <query files or directories...> [--realistic] [--quiet]
 * A query may sit next to <name>.variables.json. One line per problem, "ok <file>" per clean file; exit 1
 * when anything failed.
 */
public class MockCheck {

    static final JsonMapper JSON = JsonMapper.builder().build();
    static final HttpClient HTTP = HttpClient.newHttpClient();

    final GraphQLSchema schema;
    final boolean realistic;
    final List<String> problems = new ArrayList<>();
    Map<String, FragmentDefinition> fragments = Map.of();
    Map<String, Object> variables = Map.of();

    MockCheck(GraphQLSchema schema, boolean realistic) { this.schema = schema; this.realistic = realistic; }

    public static void main(String[] args) throws Exception {
        List<String> rest = new ArrayList<>(List.of(args));
        boolean realistic = rest.remove("--realistic"), quiet = rest.remove("--quiet");
        if (rest.size() < 3) { System.err.println("usage: MockCheck <url> <schema> <queries...> [--realistic]"); System.exit(2); }
        String url = rest.removeFirst();
        var registry = new SchemaParser().parse(Files.readString(Path.of(rest.removeFirst())));
        var schema = new SchemaGenerator().makeExecutableSchema(registry,
            RuntimeWiring.newRuntimeWiring().wiringFactory(new MockedWiringFactory()).build());
        List<Path> files = new ArrayList<>();
        for (String r : rest) {
            Path p = Path.of(r);
            if (Files.isDirectory(p)) try (var s = Files.list(p)) { s.filter(f -> f.toString().endsWith(".graphql")).sorted().forEach(files::add); }
            else files.add(p);
        }
        int bad = 0;
        for (Path f : files) {
            var check = new MockCheck(schema, realistic);
            check.run(url, f);
            if (check.problems.isEmpty()) { if (!quiet) System.out.println("ok   " + f.getFileName()); }
            else { bad++; check.problems.stream().limit(6).forEach(p -> System.out.println("FAIL " + f.getFileName() + ": " + p)); }
        }
        System.out.println(files.size() - bad + " of " + files.size() + " queries clean");
        System.exit(bad == 0 ? 0 : 1);
    }

    @SuppressWarnings("unchecked")
    void run(String url, Path file) throws Exception {
        String query = Files.readString(file);
        Path varsFile = Path.of(file.toString().replaceFirst("\\.graphql$", ".variables.json"));
        if (Files.exists(varsFile)) variables = JSON.readValue(Files.readString(varsFile), Map.class);
        Document doc = Parser.parse(query);
        List<ValidationError> invalid = new Validator().validateDocument(schema, doc, Locale.ENGLISH);
        if (!invalid.isEmpty()) { problems.add("the query itself is invalid (course bug): " + invalid.getFirst().getMessage()); return; }
        fragments = new LinkedHashMap<>();
        for (var d : doc.getDefinitionsOfType(FragmentDefinition.class)) fragments.put(d.getName(), d);
        OperationDefinition op = doc.getDefinitionsOfType(OperationDefinition.class).getFirst();

        String body = JSON.writeValueAsString(Map.of("query", query, "variables", variables));
        String first = post(url, body), second = post(url, body);
        if (first == null) return;
        JsonNode response = JSON.readTree(first);
        if (response.has("errors") && !response.get("errors").isEmpty())
            problems.add("errors in the response: " + response.get("errors").get(0).path("message").asString());
        JsonNode data = response.get("data");
        if (data == null || data.isNull()) { problems.add("no data"); return; }
        GraphQLObjectType root = switch (op.getOperation()) {
            case MUTATION -> schema.getMutationType();
            case SUBSCRIPTION -> schema.getSubscriptionType();
            default -> schema.getQueryType();
        };
        checkObject("", root, op.getSelectionSet(), data);
        if (second != null && !JSON.readTree(second).equals(response)) problems.add("not deterministic: the same request twice gave different responses");
    }

    String post(String url, String body) {
        try {
            var res = HTTP.send(HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
                .header("Accept", "application/graphql-response+json, application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) { problems.add("HTTP " + res.statusCode() + ": " + res.body().lines().findFirst().orElse("")); return null; }
            return res.body();
        } catch (Exception e) { problems.add("no response from " + url + ": " + e.getMessage()); return null; }
    }

    // --- CollectFields (spec 6.3.2), with __typename deciding fragment applicability ---------------------
    void collect(GraphQLObjectType type, SelectionSet set, Map<String, List<Field>> out, Set<String> visited) {
        for (Selection<?> s : set.getSelections()) {
            if (s instanceof Field f) out.computeIfAbsent(f.getResultKey(), k -> new ArrayList<>()).add(f);
            else if (s instanceof InlineFragment in) {
                if (in.getTypeCondition() == null || applies(type, in.getTypeCondition().getName())) collect(type, in.getSelectionSet(), out, visited);
            } else if (s instanceof FragmentSpread sp && visited.add(sp.getName())) {
                var def = fragments.get(sp.getName());
                if (applies(type, def.getTypeCondition().getName())) collect(type, def.getSelectionSet(), out, visited);
            }
        }
    }

    boolean applies(GraphQLObjectType type, String condition) {
        if (type.getName().equals(condition)) return true;
        GraphQLType t = schema.getType(condition);
        if (t instanceof GraphQLInterfaceType) return type.getInterfaces().stream().anyMatch(i -> i.getName().equals(condition));
        if (t instanceof GraphQLUnionType u) return u.getTypes().stream().anyMatch(m -> m.getName().equals(type.getName()));
        return false;
    }

    void checkObject(String path, GraphQLObjectType type, SelectionSet set, JsonNode value) {
        if (!value.isObject()) { problems.add(path + ": expected an object of type " + type.getName() + ", got " + value); return; }
        Map<String, List<Field>> fields = new LinkedHashMap<>();
        collect(type, set, fields, new HashSet<>());
        Set<String> got = new LinkedHashSet<>(value.propertyNames());
        if (!got.equals(fields.keySet())) problems.add((path.isEmpty() ? "data" : path) + ": keys " + got + ", the document selects " + fields.keySet());
        for (var e : fields.entrySet()) {
            Field f = e.getValue().getFirst();
            String p = path.isEmpty() ? e.getKey() : path + "." + e.getKey();
            JsonNode v = value.get(e.getKey());
            if (v == null) continue;
            if (f.getName().equals("__typename")) {
                if (!v.isString() || !v.asString().equals(type.getName())) problems.add(p + ": __typename " + v + " but the fields are " + type.getName() + "'s");
                continue;
            }
            GraphQLFieldDefinition def = type.getFieldDefinition(f.getName());
            if (def == null) continue;
            SelectionSet merged = merge(e.getValue());
            checkValue(p, f, def.getType(), merged, v, null);
        }
    }

    static SelectionSet merge(List<Field> fields) {
        var b = SelectionSet.newSelectionSet();
        for (Field f : fields) if (f.getSelectionSet() != null) f.getSelectionSet().getSelections().forEach(b::selection);
        return b.build();
    }

    void checkValue(String path, Field field, GraphQLType type, SelectionSet set, JsonNode v, List<JsonNode> siblings) {
        if (type instanceof GraphQLNonNull nn) {
            if (v.isNull()) { problems.add(path + ": null for non-null " + nn); return; }
            checkValue(path, field, nn.getWrappedType(), set, v, siblings);
            return;
        }
        if (v.isNull()) return;
        if (type instanceof GraphQLList list) {
            if (!v.isArray()) { problems.add(path + ": expected a list, got " + v); return; }
            if (realistic) listRules(path, field, v);
            List<JsonNode> items = new ArrayList<>();
            v.forEach(items::add);
            for (int i = 0; i < items.size(); i++) checkValue(path + "[" + i + "]", field, list.getWrappedType(), set, items.get(i), items);
            if (realistic && items.size() > 1 && unwrap(list.getWrappedType()) instanceof GraphQLScalarType) sameEverywhere(path, items);
            return;
        }
        if (type instanceof GraphQLScalarType s) { checkScalar(path, field, s, v); return; }
        if (type instanceof GraphQLEnumType en) {
            if (!v.isString() || en.getValue(v.asString()) == null) problems.add(path + ": " + v + " is not a value of enum " + en.getName());
            return;
        }
        GraphQLObjectType concrete;
        if (type instanceof GraphQLObjectType o) concrete = o;
        else {
            String tn = v.path("__typename").asString("");
            GraphQLType t = schema.getType(tn);
            if (!(t instanceof GraphQLObjectType o) || !possible(type, o)) {
                problems.add(path + ": __typename " + (tn.isEmpty() ? "missing" : tn) + " is not a possible type of " + ((GraphQLNamedType) type).getName()
                    + " (select __typename on abstract fields)");
                return;
            }
            concrete = o;
        }
        checkObject(path, concrete, set, v);
        if (realistic && v.has("id") && field.getArguments().stream().anyMatch(a -> a.getName().equals("id"))) {
            Object arg = argument(field, "id");
            if (arg != null && !arg.toString().equals(v.get("id").asString())) problems.add(path + ".id: " + v.get("id") + " but the query asked for id " + arg);
        }
    }

    boolean possible(GraphQLType abstractType, GraphQLObjectType o) { return schema.isPossibleType((graphql.schema.GraphQLNamedType) abstractType, o); }

    static GraphQLType unwrap(GraphQLType t) {
        while (t instanceof GraphQLNonNull || t instanceof GraphQLList) t = t instanceof GraphQLNonNull n ? n.getWrappedType() : ((GraphQLList) t).getWrappedType();
        return t;
    }

    void checkScalar(String path, Field field, GraphQLScalarType s, JsonNode v) {
        String name = field.getName(), lower = name.toLowerCase(Locale.ROOT);
        switch (s.getName()) {
            case "Int" -> { if (!v.isIntegralNumber() || !v.canConvertToInt()) problems.add(path + ": " + v + " is not an Int"); }
            case "Float" -> { if (!v.isNumber()) problems.add(path + ": " + v + " is not a Float"); }
            case "Boolean" -> { if (!v.isBoolean()) problems.add(path + ": " + v + " is not a Boolean"); }
            case "String", "ID" -> { if (!v.isString()) problems.add(path + ": " + v + " is not a " + s.getName() + " (IDs are serialised as strings)"); }
            default -> { }
        }
        if (!realistic) return;
        String text = v.isString() ? v.asString() : v.toString();
        if (v.isString() && (text.isBlank() || text.equals("String") || text.equals("ID") || text.equalsIgnoreCase(name) || text.equals("mock")))
            problems.add(path + ": " + v + " is a placeholder, not realistic data");
        if (lower.endsWith("email") && !(text.contains("@") && text.indexOf('.', text.indexOf('@')) > 0)) problems.add(path + ": " + v + " does not look like an email");
        if ((lower.endsWith("url") || lower.endsWith("uri") || lower.equals("homepage")) && !text.startsWith("http")) problems.add(path + ": " + v + " does not look like a URL");
        if ((name.endsWith("At") || lower.endsWith("date") || s.getName().equals("DateTime") || s.getName().equals("Date")) && v.isString() && !isoDate(text))
            problems.add(path + ": " + v + " is not an ISO 8601 date or time");
        if (lower.endsWith("count") && v.isNumber() && v.asDouble() < 0) problems.add(path + ": a count of " + v);
    }

    static boolean isoDate(String s) {
        for (var parse : List.<java.util.function.Consumer<String>>of(Instant::parse, OffsetDateTime::parse, LocalDate::parse))
            try { parse.accept(s); return true; } catch (Exception ignored) { }
        return false;
    }

    void listRules(String path, Field field, JsonNode list) {
        Object n = null;
        for (String a : List.of("first", "last", "limit", "take")) if ((n = argument(field, a)) != null) break;
        if (n instanceof Number num && num.intValue() <= 50) {
            if (list.size() != num.intValue()) problems.add(path + ": " + list.size() + " items, but the query asked for " + num);
        } else if (list.isEmpty() || list.size() > 5) problems.add(path + ": " + list.size() + " items (realistic lists have 1 to 5 when the query does not say)");
        Set<String> ids = new HashSet<>();
        for (JsonNode item : list) if (item.has("id") && !ids.add(item.get("id").asString())) { problems.add(path + ": id " + item.get("id") + " twice in one list"); break; }
        // Per-field sameness across objects in the list: every item with the same name or title is not realistic.
        if (list.size() > 1 && list.get(0).isObject())
            for (String key : list.get(0).propertyNames()) {
                List<JsonNode> column = new ArrayList<>();
                for (JsonNode item : list) if (item.has(key)) column.add(item.get(key));
                boolean enumLike = column.stream().allMatch(c -> c.isString() && c.asString().matches("[A-Z][A-Z0-9_]*"));
                if (column.size() == list.size() && (column.get(0).isString() || column.get(0).isIntegralNumber()) && !enumLike
                    && !key.equals("__typename")) sameEverywhere(path + "[*]." + key, column);
            }
    }

    void sameEverywhere(String path, List<JsonNode> values) {
        if (values.size() > 2 && values.stream().distinct().count() == 1) problems.add(path + ": the same value " + values.getFirst() + " in every item");
    }

    Object argument(Field f, String name) {
        for (Argument a : f.getArguments()) {
            if (!a.getName().equals(name)) continue;
            if (a.getValue() instanceof IntValue i) return i.getValue().intValue();
            if (a.getValue() instanceof StringValue s) return s.getValue();
            if (a.getValue() instanceof VariableReference r) return variables.get(r.getName());
        }
        return null;
    }
}

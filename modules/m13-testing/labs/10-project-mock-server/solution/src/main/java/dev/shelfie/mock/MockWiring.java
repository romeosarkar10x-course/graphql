package dev.shelfie.mock;

import graphql.schema.Coercing;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLType;
import graphql.schema.TypeResolver;
import graphql.schema.idl.FieldWiringEnvironment;
import graphql.schema.idl.InterfaceWiringEnvironment;
import graphql.schema.idl.ScalarInfo;
import graphql.schema.idl.ScalarWiringEnvironment;
import graphql.schema.idl.UnionWiringEnvironment;
import graphql.schema.idl.WiringFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fake data for any schema, with no resolvers written by hand. Every value is a function of (seed, path), so
 * the same query gives the same answer and different positions give different values. Field names steer the
 * values (email, url, *At, *Count, name, title...), id arguments are echoed, first/last/limit size lists.
 */
public class MockWiring implements WiringFactory {

    static final String[] FIRST = {"Asha", "Ben", "Chloe", "Dev", "Elena", "Farid", "Grace", "Hugo", "Ines", "Jonas", "Kemi", "Liu"};
    static final String[] LAST = {"Rao", "Okafor", "Martin", "Patel", "Petrova", "Haddad", "Kim", "Silva", "Duarte", "Berg", "Adeyemi", "Wei"};
    static final String[] ADJ = {"Silent", "Seventh", "Hidden", "Last", "Glass", "Northern", "Paper", "Quiet", "Burning", "Winter"};
    static final String[] NOUN = {"Harbour", "Archive", "Garden", "Orchard", "Lighthouse", "Library", "River", "Kingdom", "Clock", "Map"};
    static final String[] WORDS = {"a", "slow", "book", "about", "memory", "and", "the", "sea", "with", "one", "perfect", "ending",
        "read", "it", "twice", "quietly", "brilliant", "characters", "chapter", "worth"};
    static final String[] PLACES = {"Online", "Bengaluru", "Lagos", "Lisbon", "Paris", "Seoul", "Oslo", "Toronto"};

    private final long seed;

    public MockWiring(long seed) { this.seed = seed; }

    // ----- WiringFactory: we provide every data fetcher, every type resolver and every custom scalar -----

    @Override public boolean providesDataFetcher(FieldWiringEnvironment env) { return true; }
    @Override public DataFetcher<?> getDataFetcher(FieldWiringEnvironment env) { return this::fetch; }
    @Override public boolean providesTypeResolver(InterfaceWiringEnvironment env) { return true; }
    @Override public TypeResolver getTypeResolver(InterfaceWiringEnvironment env) { return this::resolve; }
    @Override public boolean providesTypeResolver(UnionWiringEnvironment env) { return true; }
    @Override public TypeResolver getTypeResolver(UnionWiringEnvironment env) { return this::resolve; }

    @Override
    public boolean providesScalar(ScalarWiringEnvironment env) { return !ScalarInfo.isGraphqlSpecifiedScalar(env.getScalarTypeDefinition().getName()); }

    /** Custom scalars pass values through: the fetcher already made a sensible value for the scalar's name. */
    @Override
    public GraphQLScalarType getScalar(ScalarWiringEnvironment env) {
        return GraphQLScalarType.newScalar().name(env.getScalarTypeDefinition().getName()).coercing(new Coercing<Object, Object>() {
            @Override public Object serialize(Object v, graphql.GraphQLContext c, Locale l) { return v; }
            @Override public Object parseValue(Object v, graphql.GraphQLContext c, Locale l) { return v; }
            @Override public Object parseLiteral(graphql.language.Value<?> v, graphql.execution.CoercedVariables vars, graphql.GraphQLContext c, Locale l) {
                return v instanceof graphql.language.StringValue s ? s.getValue() : v.toString();
            }
        }).build();
    }

    // ----- data ---------------------------------------------------------------------------------------

    /** A position in the response, e.g. "club/members/1/name", hashed with the seed. */
    long hash(String path) {
        long h = seed * 0x9E3779B97F4A7C15L;
        for (int i = 0; i < path.length(); i++) h = (h ^ path.charAt(i)) * 0x100000001B3L;
        return h ^ (h >>> 31);
    }

    int pick(String path, int n) { return (int) Math.floorMod(hash(path), (long) n); }

    Object fetch(DataFetchingEnvironment env) {
        String name = env.getField().getName();
        // A parent that pinned this field (an echoed id argument) wins.
        if (env.getSource() instanceof Map<?, ?> pinned && pinned.containsKey(name)) return pinned.get(name);
        String path = env.getExecutionStepInfo().getPath().toString();
        return value(path, name, env.getFieldType(), env.getArguments(), parentName(env));
    }

    static String parentName(DataFetchingEnvironment env) {
        return env.getParentType() instanceof GraphQLNamedType n ? n.getName() : "";
    }

    Object value(String path, String field, GraphQLType type, Map<String, Object> args, String parent) {
        if (type instanceof GraphQLNonNull nn) return value(path, field, nn.getWrappedType(), args, parent);
        if (type instanceof GraphQLList list) {
            int n = 2 + pick(path + "#size", 3);
            for (String a : List.of("first", "last", "limit", "take"))
                if (args.get(a) instanceof Integer i) { n = Math.max(0, Math.min(i, 50)); break; }
            List<Object> items = new ArrayList<>();
            for (int i = 0; i < n; i++) items.add(value(path + "/" + i, field, list.getWrappedType(), Map.of(), parent));
            return items;
        }
        if (type instanceof GraphQLEnumType en) return en.getValues().get(pick(path, en.getValues().size())).getName();
        if (type instanceof GraphQLScalarType s) return scalar(path, field, s.getName(), parent);
        // An object, interface or union: an object whose fields are fetched later. Pin an echoed id.
        Map<String, Object> obj = new HashMap<>();
        if (args.get("id") != null) obj.put("id", args.get("id").toString());
        obj.put("__path", path);
        return obj;
    }

    Object scalar(String path, String field, String scalar, String parent) {
        String f = field.toLowerCase(Locale.ROOT);
        long h = hash(path);
        int n = (int) Math.floorMod(h, 100_000L);
        switch (scalar) {
            case "Int" -> {
                if (f.endsWith("count") || f.startsWith("total")) return n % 500;
                if (f.contains("year")) return 1900 + n % 127;
                if (f.contains("rating") || f.contains("stars")) return 1 + n % 5;
                return 1 + n % 100;
            }
            case "Float" -> {
                if (f.contains("rating") || f.contains("average")) return 1 + (n % 401) / 100.0;
                return (n % 10_000) / 100.0;
            }
            case "Boolean" -> { return (h & 1) == 0; }
            case "ID" -> { return parent.isEmpty() ? "id-" + n : Character.toLowerCase(parent.charAt(0)) + parent.substring(1) + "-" + n; }
            default -> { return text(f, scalar.toLowerCase(Locale.ROOT), parent, n); }
        }
    }

    String text(String f, String scalar, String parent, int n) {
        String person = FIRST[n % FIRST.length] + " " + LAST[(n / 13) % LAST.length];
        if (f.endsWith("email") || scalar.contains("email")) return FIRST[n % FIRST.length].toLowerCase(Locale.ROOT) + n + "@example.com";
        if (f.endsWith("url") || f.endsWith("uri") || f.equals("homepage") || f.endsWith("website") || scalar.contains("url")) return "https://example.com/" + parent.toLowerCase(Locale.ROOT) + "/" + n;
        if (f.endsWith("at") && !f.equals("at") || f.endsWith("date") || scalar.contains("date") || scalar.contains("time"))
            return Instant.parse("2026-01-01T00:00:00Z").plusSeconds((long) n * 263).toString();
        if (f.equals("id") || scalar.equals("id")) return parent.toLowerCase(Locale.ROOT) + "-" + n;
        if (f.equals("handle") || f.equals("username") || f.equals("login")) return FIRST[n % FIRST.length].toLowerCase(Locale.ROOT) + (n % 97);
        if (f.equals("name") && (parent.equals("User") || parent.equals("Author") || parent.equals("Person") || parent.equals("Member"))) return person;
        if (f.contains("author") || f.contains("person")) return person;
        if (f.equals("name") || f.equals("title")) return "The " + ADJ[n % ADJ.length] + " " + NOUN[(n / 7) % NOUN.length];
        if (f.contains("location") || f.contains("city") || f.contains("place")) return PLACES[n % PLACES.length];
        if (f.contains("isbn")) return "978" + String.format("%010d", (long) n * 7919 % 10_000_000_000L);
        if (f.contains("description") || f.contains("body") || f.contains("text") || f.contains("bio") || f.contains("summary")) {
            var b = new StringBuilder();
            for (int i = 0; i < 6 + n % 6; i++) b.append(i == 0 ? "" : " ").append(WORDS[(n / (i + 1) + i * 7) % WORDS.length]);
            return Character.toUpperCase(b.charAt(0)) + b.substring(1) + ".";
        }
        return ADJ[n % ADJ.length] + " " + NOUN[(n / 7) % NOUN.length].toLowerCase(Locale.ROOT);
    }

    /** An abstract type's concrete type, picked by position, so a list of SearchResult mixes its members. */
    GraphQLObjectType resolve(graphql.TypeResolutionEnvironment env) {
        String path = env.getObject() instanceof Map<?, ?> m ? String.valueOf(m.get("__path")) : "";
        GraphQLType t = graphql.schema.GraphQLTypeUtil.unwrapAll(env.getFieldType());
        List<GraphQLObjectType> options = new ArrayList<>(t instanceof graphql.schema.GraphQLUnionType u
            ? u.getTypes().stream().map(GraphQLObjectType.class::cast).toList()
            : env.getSchema().getImplementations((graphql.schema.GraphQLInterfaceType) t));
        options.sort(java.util.Comparator.comparing(GraphQLObjectType::getName));
        return options.get(pick(path + "#type", options.size()));
    }
}

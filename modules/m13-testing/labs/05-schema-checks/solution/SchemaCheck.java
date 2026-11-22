import graphql.ExecutionInput;
import graphql.ParseAndValidate;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.VariableDefinition;
import graphql.parser.Parser;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.diff.DiffEvent;
import graphql.schema.diff.DiffLevel;
import graphql.schema.diff.SchemaDiff;
import graphql.schema.diff.SchemaDiffSet;
import graphql.schema.diff.reporting.CapturingReporter;
import graphql.schema.idl.MockedWiringFactory;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The schema gate for CI (lab 13.5): compares the committed schema with the new one and checks every known
 * client operation against the new one, like a registry check (Hive, GraphOS) with the clients in the repo.
 *
 *   bin/course java SchemaCheck.java <old.graphqls> <new.graphqls> <clients-dir>
 *
 * Fails (exit 1) when a breaking change touches something a client operation uses, or when any client
 * operation no longer validates. A breaking change no known client uses is reported and allowed, and a
 * changed default only counts for operations that leave the argument out.
 */
public class SchemaCheck {

    record Op(String name, Path file, Set<String> uses) {}

    public static void main(String[] args) throws Exception {
        String oldSdl = Files.readString(Path.of(args[0])), newSdl = Files.readString(Path.of(args[1]));
        GraphQLSchema oldSchema = schema(oldSdl), newSchema = schema(newSdl);
        List<Op> ops = new ArrayList<>();
        List<Path> files;
        try (Stream<Path> s = Files.walk(Path.of(args[2]))) { files = s.filter(p -> p.toString().endsWith(".graphql")).sorted().toList(); }

        // Usage comes from the old schema: that is the one the clients were written against.
        for (Path f : files) {
            Document doc = Parser.parse(Files.readString(f));
            for (OperationDefinition op : doc.getDefinitionsOfType(OperationDefinition.class))
                ops.add(new Op(op.getName(), f, uses(oldSchema, doc, op)));
        }

        var reporter = new CapturingReporter();
        new SchemaDiff().diffSchema(SchemaDiffSet.diffSetFromSdl(oldSdl, newSdl), reporter);
        int breakingUsed = 0, breakingUnused = 0, dangerous = 0;
        for (DiffEvent e : reporter.getEvents()) {
            if (e.getLevel() == DiffLevel.INFO) continue;
            String coordinate = coordinate(e);
            List<Op> users = ops.stream().filter(op -> touches(op.uses(), e, coordinate)).toList();
            System.out.println(e.getLevel() + " " + coordinate + ": " + e.getReasonMsg());
            System.out.println("  why: " + why(e));
            if (users.isEmpty()) System.out.println("  unused by every known client operation");
            for (Op op : users) System.out.println("  used by " + op.name() + " (" + op.file() + ")");
            if (e.getLevel() == DiffLevel.BREAKING) { if (users.isEmpty()) breakingUnused++; else breakingUsed++; }
            else dangerous++;
        }

        int invalid = 0;
        for (Path f : files) {
            String text = Files.readString(f);
            for (OperationDefinition op : Parser.parse(text).getDefinitionsOfType(OperationDefinition.class)) {
                var result = ParseAndValidate.parseAndValidate(newSchema, ExecutionInput.newExecutionInput(text).operationName(op.getName()).build());
                if (!result.isFailure()) continue;
                invalid++;
                for (var err : result.getErrors()) {
                    var loc = err.getLocations().isEmpty() ? "" : " at " + f + ":" + err.getLocations().getFirst().getLine() + ":" + err.getLocations().getFirst().getColumn();
                    System.out.println("INVALID " + op.getName() + " (" + f + "): " + err.getMessage() + loc);
                }
            }
        }
        boolean failed = breakingUsed > 0 || invalid > 0;
        System.out.printf("verdict: %s (%d breaking and used, %d breaking but unused, %d dangerous, %d invalid operations of %d)%n",
            failed ? "FAIL" : "PASS", breakingUsed, breakingUnused, dangerous, invalid, ops.size());
        System.exit(failed ? 1 : 0);
    }

    static GraphQLSchema schema(String sdl) {
        return new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl),
            RuntimeWiring.newRuntimeWiring().wiringFactory(new MockedWiringFactory()).build());
    }

    /** Type.field for fields and input fields, Enum.VALUE for enum values, Type.field(arg:) for argument changes. */
    static String coordinate(DiffEvent e) {
        if ("Enum".equals(String.valueOf(e.getTypeKind())) && !e.getComponents().isEmpty()) return e.getTypeName() + "." + e.getComponents().getFirst();
        if (e.getFieldName() == null) return e.getTypeName();
        String c = e.getTypeName() + "." + e.getFieldName();
        if (e.getCategory().name().equals("DIFFERENT") && e.getComponents().size() == 1) c += "(" + e.getComponents().getFirst() + ":)";
        return c;
    }

    /** What a client sees: the reason in client terms, not schema terms. */
    static String why(DiffEvent e) {
        return switch (e.getCategory().name()) {
            case "MISSING" -> "Enum".equals(String.valueOf(e.getTypeKind()))
                ? "operations that send this value fail validation; clients that read it will not see it again"
                : "an operation that selects or sends it fails validation, before any resolver runs";
            case "STRICTER" -> "requests that omit it or send null are now rejected";
            case "INVALID" -> "the value has a new type; clients decode the old one";
            case "ADDITION" -> "clients that switch over every value meet one they have never seen";
            case "DIFFERENT" -> "requests that rely on the default now get different data with no error";
            case "DEPRECATION_ADDED" -> "still works, but clients should move off it before it is removed";
            default -> e.getCategory().name().toLowerCase();
        };
    }

    /**
     * Does an operation that uses these coordinates notice the change? A changed default only reaches operations
     * that leave the argument out; everything else reaches anyone who uses the field, or the type.
     */
    static boolean touches(Set<String> uses, DiffEvent e, String coordinate) {
        if (coordinate.endsWith(":)")) return uses.contains(coordinate.replaceFirst("\\(.*", "")) && !uses.contains(coordinate);
        return uses.contains(coordinate) || uses.contains(e.getTypeName());
    }

    /** Coordinates an operation uses: Type.field for every selected field, plus every input and enum type it touches. */
    static Set<String> uses(GraphQLSchema schema, Document doc, OperationDefinition op) {
        Set<String> uses = new LinkedHashSet<>();
        Map<String, FragmentDefinition> fragments = new LinkedHashMap<>();
        doc.getDefinitionsOfType(FragmentDefinition.class).forEach(f -> fragments.put(f.getName(), f));
        GraphQLObjectType root = switch (op.getOperation()) {
            case MUTATION -> schema.getMutationType();
            case SUBSCRIPTION -> schema.getSubscriptionType();
            default -> schema.getQueryType();
        };
        walk(schema, root, op.getSelectionSet(), fragments, uses, new LinkedHashSet<>());
        for (VariableDefinition v : op.getVariableDefinitions()) {
            String name = graphql.schema.idl.TypeUtil.unwrapAll(v.getType()).getName();
            if (schema.getType(name) instanceof GraphQLNamedType t) inputs(t, uses);
        }
        return uses;
    }

    static void walk(GraphQLSchema schema, GraphQLNamedType parent, SelectionSet set, Map<String, FragmentDefinition> fragments,
                     Set<String> uses, Set<String> seen) {
        if (set == null || !(parent instanceof GraphQLFieldsContainer container)) return;
        for (Selection<?> sel : set.getSelections()) {
            if (sel instanceof Field f) {
                if (f.getName().startsWith("__")) continue;
                GraphQLFieldDefinition def = container.getFieldDefinition(f.getName());
                if (def == null) continue;
                uses.add(container.getName() + "." + f.getName());
                f.getArguments().forEach(a -> uses.add(container.getName() + "." + f.getName() + "(" + a.getName() + ":)"));
                def.getArguments().forEach(a -> inputs((GraphQLNamedType) GraphQLTypeUtil.unwrapAll(a.getType()), uses));
                GraphQLNamedType type = (GraphQLNamedType) GraphQLTypeUtil.unwrapAll(def.getType());
                if (type instanceof GraphQLEnumType) uses.add(type.getName());
                walk(schema, type, f.getSelectionSet(), fragments, uses, seen);
            } else if (sel instanceof InlineFragment in) {
                GraphQLNamedType t = in.getTypeCondition() == null ? parent : (GraphQLNamedType) schema.getType(in.getTypeCondition().getName());
                walk(schema, t, in.getSelectionSet(), fragments, uses, seen);
            } else if (sel instanceof FragmentSpread sp && seen.add(sp.getName()) && fragments.containsKey(sp.getName())) {
                FragmentDefinition fd = fragments.get(sp.getName());
                walk(schema, (GraphQLNamedType) schema.getType(fd.getTypeCondition().getName()), fd.getSelectionSet(), fragments, uses, seen);
            }
        }
    }

    static void inputs(GraphQLNamedType t, Set<String> uses) {
        if (t instanceof GraphQLEnumType) { uses.add(t.getName()); return; }
        if (t instanceof GraphQLInputObjectType in && uses.add(in.getName()))
            in.getFieldDefinitions().forEach(f -> { uses.add(in.getName() + "." + f.getName()); inputs(GraphQLTypeUtil.unwrapAll(f.getType()), uses); });
    }
}

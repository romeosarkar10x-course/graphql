package dev.shelfie.mini;

import dev.shelfie.mini.Ast.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A schema built from SDL (spec 3): the user's types and directives, the five built-in scalars, the built-in
 * directives, and the introspection types (4.2), which are themselves written in SDL and parsed by our own parser.
 */
public final class Schema {
    /** Spec 3.13 and 4.2, as SDL. Parsed once, added to every schema. */
    static final String BUILT_INS = """
        scalar Int
        scalar Float
        scalar String
        scalar Boolean
        scalar ID
        directive @include(if: Boolean!) on FIELD | FRAGMENT_SPREAD | INLINE_FRAGMENT
        directive @skip(if: Boolean!) on FIELD | FRAGMENT_SPREAD | INLINE_FRAGMENT
        directive @deprecated(reason: String! = "No longer supported") on FIELD_DEFINITION | ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION | ENUM_VALUE
        directive @specifiedBy(url: String!) on SCALAR
        directive @oneOf on INPUT_OBJECT
        type __Schema { description: String types: [__Type!]! queryType: __Type! mutationType: __Type
          subscriptionType: __Type directives: [__Directive!]! }
        type __Type { kind: __TypeKind! name: String description: String specifiedByURL: String
          fields(includeDeprecated: Boolean! = false): [__Field!] interfaces: [__Type!] possibleTypes: [__Type!]
          enumValues(includeDeprecated: Boolean! = false): [__EnumValue!]
          inputFields(includeDeprecated: Boolean! = false): [__InputValue!] ofType: __Type isOneOf: Boolean }
        enum __TypeKind { SCALAR OBJECT INTERFACE UNION ENUM INPUT_OBJECT LIST NON_NULL }
        type __Field { name: String! description: String args(includeDeprecated: Boolean! = false): [__InputValue!]!
          type: __Type! isDeprecated: Boolean! deprecationReason: String }
        type __InputValue { name: String! description: String type: __Type! defaultValue: String
          isDeprecated: Boolean! deprecationReason: String }
        type __EnumValue { name: String! description: String isDeprecated: Boolean! deprecationReason: String }
        type __Directive { name: String! description: String isRepeatable: Boolean! locations: [__DirectiveLocation!]!
          args(includeDeprecated: Boolean! = false): [__InputValue!]! }
        enum __DirectiveLocation { QUERY MUTATION SUBSCRIPTION FIELD FRAGMENT_DEFINITION FRAGMENT_SPREAD INLINE_FRAGMENT
          VARIABLE_DEFINITION SCHEMA SCALAR OBJECT FIELD_DEFINITION ARGUMENT_DEFINITION INTERFACE UNION ENUM ENUM_VALUE
          INPUT_OBJECT INPUT_FIELD_DEFINITION }
        """;
    static final List<String> SCALARS = List.of("Int", "Float", "String", "Boolean", "ID");
    static final FieldDef TYPENAME = new FieldDef(null, "__typename", List.of(), new NonNullType(new NamedType("String")), List.of());
    static final FieldDef SCHEMA = new FieldDef(null, "__schema", List.of(), new NonNullType(new NamedType("__Schema")), List.of());
    static final FieldDef TYPE = new FieldDef(null, "__type",
            List.of(new InputValueDef(null, "name", new NonNullType(new NamedType("String")), null, List.of())), new NamedType("__Type"), List.of());

    /** Every named type, user types first in SDL order, then built-ins. */
    public final Map<String, Definition> types = new LinkedHashMap<>();
    public final Map<String, DirectiveDef> directives = new LinkedHashMap<>();
    public final String description;
    public final String query, mutation, subscription;

    public Schema(String sdl) {
        List<Definition> defs = new ArrayList<>(Parser.parse(sdl).definitions());
        defs.addAll(Parser.parse(BUILT_INS).definitions());
        String description = null;
        Map<String, String> roots = new LinkedHashMap<>();
        List<Definition> extensions = new ArrayList<>();
        for (Definition d : defs) {
            switch (d) {
                case SchemaDef s -> { description = s.description(); s.operations().forEach(o -> roots.put(o.operation(), o.type())); }
                case DirectiveDef dd -> directives.putIfAbsent(dd.name(), dd);
                case Extension e -> extensions.add(e.definition());
                case Operation o -> throw new IllegalArgumentException("SDL may not contain operations");
                case Fragment f -> throw new IllegalArgumentException("SDL may not contain fragments");
                default -> { if (types.putIfAbsent(name(d), d) != null && !SCALARS.contains(name(d))) throw new IllegalArgumentException("type " + name(d) + " defined twice"); }
            }
        }
        for (Definition e : extensions) extend(e, roots);
        this.description = description;
        query = roots.getOrDefault("query", types.containsKey("Query") ? "Query" : null);
        mutation = roots.getOrDefault("mutation", roots.isEmpty() && types.containsKey("Mutation") ? "Mutation" : null);
        subscription = roots.getOrDefault("subscription", roots.isEmpty() && types.containsKey("Subscription") ? "Subscription" : null);
        if (query == null) throw new IllegalArgumentException("the schema has no query root type");
    }

    /** Merges an extension into the type it extends (3.4.3, 3.6.3 and so on). */
    private void extend(Definition e, Map<String, String> roots) {
        if (e instanceof SchemaDef s) { s.operations().forEach(o -> roots.put(o.operation(), o.type())); return; }
        Definition base = types.get(name(e));
        if (base == null) throw new IllegalArgumentException("cannot extend unknown type " + name(e));
        types.put(name(e), switch (base) {
            case ObjectDef b when e instanceof ObjectDef x -> new ObjectDef(b.description(), b.name(), concat(b.interfaces(), x.interfaces()), concat(b.directives(), x.directives()), concat(b.fields(), x.fields()));
            case InterfaceDef b when e instanceof InterfaceDef x -> new InterfaceDef(b.description(), b.name(), concat(b.interfaces(), x.interfaces()), concat(b.directives(), x.directives()), concat(b.fields(), x.fields()));
            case UnionDef b when e instanceof UnionDef x -> new UnionDef(b.description(), b.name(), concat(b.directives(), x.directives()), concat(b.members(), x.members()));
            case EnumDef b when e instanceof EnumDef x -> new EnumDef(b.description(), b.name(), concat(b.directives(), x.directives()), concat(b.values(), x.values()));
            case InputDef b when e instanceof InputDef x -> new InputDef(b.description(), b.name(), concat(b.directives(), x.directives()), concat(b.fields(), x.fields()));
            case ScalarDef b when e instanceof ScalarDef x -> new ScalarDef(b.description(), b.name(), concat(b.directives(), x.directives()));
            default -> throw new IllegalArgumentException("extension of " + name(e) + " does not match its kind");
        });
    }

    private static <T> List<T> concat(List<T> a, List<T> b) {
        List<T> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }

    // ---- lookups

    public static String name(Definition d) {
        return switch (d) {
            case ScalarDef s -> s.name();
            case ObjectDef o -> o.name();
            case InterfaceDef i -> i.name();
            case UnionDef u -> u.name();
            case EnumDef e -> e.name();
            case InputDef i -> i.name();
            case DirectiveDef d2 -> d2.name();
            case Extension x -> name(x.definition());
            default -> null;
        };
    }

    /** __TypeKind of a named type. */
    public String kind(String type) {
        return switch (types.get(type)) {
            case ScalarDef s -> "SCALAR";
            case ObjectDef o -> "OBJECT";
            case InterfaceDef i -> "INTERFACE";
            case UnionDef u -> "UNION";
            case EnumDef e -> "ENUM";
            case InputDef i -> "INPUT_OBJECT";
            case null, default -> null;
        };
    }

    public List<FieldDef> fields(String type) {
        return switch (types.get(type)) {
            case ObjectDef o -> o.fields();
            case InterfaceDef i -> i.fields();
            case null, default -> List.of();
        };
    }

    /** The field named on a composite type, including the meta-fields (4.4); null when there is none. */
    public FieldDef field(String type, String name) {
        if (name.equals("__typename") && isComposite(type)) return TYPENAME;
        if (type.equals(query) && name.equals("__schema")) return SCHEMA;
        if (type.equals(query) && name.equals("__type")) return TYPE;
        for (FieldDef f : fields(type)) if (f.name().equals(name)) return f;
        return null;
    }

    public List<String> interfaces(String type) {
        return switch (types.get(type)) {
            case ObjectDef o -> o.interfaces();
            case InterfaceDef i -> i.interfaces();
            case null, default -> List.of();
        };
    }

    /** The object types an abstract type stands for, or the type itself for an object type (GetPossibleTypes). */
    public List<String> possibleTypes(String type) {
        return switch (types.get(type)) {
            case UnionDef u -> u.members();
            case InterfaceDef i -> types.values().stream().filter(d -> d instanceof ObjectDef o && o.interfaces().contains(type)).map(Schema::name).toList();
            case ObjectDef o -> List.of(type);
            case null, default -> List.of();
        };
    }

    public boolean isComposite(String type) {
        return types.get(type) instanceof ObjectDef || types.get(type) instanceof InterfaceDef || types.get(type) instanceof UnionDef;
    }

    public boolean isAbstract(String type) { return types.get(type) instanceof InterfaceDef || types.get(type) instanceof UnionDef; }
    public boolean isLeaf(String type) { return types.get(type) instanceof ScalarDef || types.get(type) instanceof EnumDef; }
    public boolean isInput(String type) { return isLeaf(type) || types.get(type) instanceof InputDef; }

    public String root(String operationType) {
        return switch (operationType) {
            case "query" -> query;
            case "mutation" -> mutation;
            default -> subscription;
        };
    }

    // ---- type references

    public static String named(Type t) {
        return switch (t) {
            case NamedType n -> n.name();
            case ListType l -> named(l.of());
            case NonNullType n -> named(n.of());
        };
    }

    public static String print(Type t) {
        return switch (t) {
            case NamedType n -> n.name();
            case ListType l -> "[" + print(l.of()) + "]";
            case NonNullType n -> print(n.of()) + "!";
        };
    }

    public static Type nullable(Type t) { return t instanceof NonNullType n ? n.of() : t; }
}

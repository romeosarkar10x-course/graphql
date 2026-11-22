package dev.shelfie.mini;

import dev.shelfie.mini.Ast.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Introspection (spec 4) as ordinary resolvers over the schema. The introspection types are part of every schema
 * (Schema.BUILT_INS); their values are the schema's own definitions: a __Type is a TypeRef, a __Field a FieldDef, an
 * __InputValue an InputValueDef, an __EnumValue an EnumValueDef, a __Directive a DirectiveDef.
 */
public final class Introspection {
    public static final Set<String> BUILT_IN_DIRECTIVES = Set.of("include", "skip", "deprecated", "specifiedBy", "oneOf");

    /** A type reference: a named type, or a list or non-null wrapper around one. */
    public record TypeRef(Type type) {}

    private Introspection() {}

    static Object resolve(Schema schema, String parent, String field, Object source, Map<String, Object> args) {
        boolean all = Boolean.TRUE.equals(args.get("includeDeprecated"));
        return switch (field) {
                case "__schema" -> schema;
                case "__type" -> schema.types.containsKey((String) args.get("name")) ? named((String) args.get("name")) : null;
                default -> switch (source) {
                    case Schema s -> schemaField(s, field);
                    case TypeRef t -> typeField(schema, t, field, all);
                    case FieldDef f -> switch (field) {
                        case "name" -> f.name();
                        case "description" -> f.description();
                        case "args" -> f.arguments().stream().filter(a -> all || deprecation(a.directives()) == null).toList();
                        case "type" -> new TypeRef(f.type());
                        case "isDeprecated" -> deprecation(f.directives()) != null;
                        case "deprecationReason" -> deprecation(f.directives());
                        default -> null;
                    };
                    case InputValueDef v -> switch (field) {
                        case "name" -> v.name();
                        case "description" -> v.description();
                        case "type" -> new TypeRef(v.type());
                        case "defaultValue" -> v.defaultValue() == null ? null : Printer.value(v.defaultValue());
                        case "isDeprecated" -> deprecation(v.directives()) != null;
                        case "deprecationReason" -> deprecation(v.directives());
                        default -> null;
                    };
                    case EnumValueDef v -> switch (field) {
                        case "name" -> v.name();
                        case "description" -> v.description();
                        case "isDeprecated" -> deprecation(v.directives()) != null;
                        case "deprecationReason" -> deprecation(v.directives());
                        default -> null;
                    };
                    case DirectiveDef d -> switch (field) {
                        case "name" -> d.name();
                        case "description" -> d.description();
                        case "isRepeatable" -> d.repeatable();
                        case "locations" -> d.locations();
                        case "args" -> d.arguments().stream().filter(a -> all || deprecation(a.directives()) == null).toList();
                        default -> null;
                    };
                    default -> null;
                };
        };
    }

    private static TypeRef named(String name) { return new TypeRef(new NamedType(name)); }

    private static Object schemaField(Schema s, String field) {
        return switch (field) {
            case "description" -> s.description;
            case "types" -> s.types.keySet().stream().map(Introspection::named).toList();
            case "queryType" -> named(s.query);
            case "mutationType" -> s.mutation == null ? null : named(s.mutation);
            case "subscriptionType" -> s.subscription == null ? null : named(s.subscription);
            case "directives" -> new ArrayList<>(s.directives.values());
            default -> null;
        };
    }

    private static Object typeField(Schema schema, TypeRef ref, String field, boolean all) {
        if (!(ref.type() instanceof NamedType n)) {
            Type of = ref.type() instanceof ListType l ? l.of() : ((NonNullType) ref.type()).of();
            return switch (field) {
                case "kind" -> ref.type() instanceof ListType ? "LIST" : "NON_NULL";
                case "ofType" -> new TypeRef(of);
                default -> null;
            };
        }
        String name = n.name(), kind = schema.kind(name);
        Definition def = schema.types.get(name);
        return switch (field) {
            case "kind" -> kind;
            case "name" -> name;
            case "description" -> switch (def) {
                case ScalarDef s -> s.description();
                case ObjectDef o -> o.description();
                case InterfaceDef i -> i.description();
                case UnionDef u -> u.description();
                case EnumDef e -> e.description();
                case InputDef i -> i.description();
                default -> null;
            };
            case "specifiedByURL" -> def instanceof ScalarDef s ? s.directives().stream().filter(d -> d.name().equals("specifiedBy"))
                    .flatMap(d -> d.arguments().stream()).map(a -> ((StringV) a.value()).value()).findFirst().orElse(null) : null;
            case "fields" -> kind.equals("OBJECT") || kind.equals("INTERFACE")
                    ? schema.fields(name).stream().filter(f -> all || deprecation(f.directives()) == null).toList() : null;
            case "interfaces" -> kind.equals("OBJECT") || kind.equals("INTERFACE") ? schema.interfaces(name).stream().map(Introspection::named).toList() : null;
            case "possibleTypes" -> schema.isAbstract(name) ? schema.possibleTypes(name).stream().map(Introspection::named).toList() : null;
            case "enumValues" -> def instanceof EnumDef e ? e.values().stream().filter(v -> all || deprecation(v.directives()) == null).toList() : null;
            case "inputFields" -> def instanceof InputDef i ? i.fields().stream().filter(f -> all || deprecation(f.directives()) == null).toList() : null;
            case "isOneOf" -> def instanceof InputDef i ? i.directives().stream().anyMatch(d -> d.name().equals("oneOf")) : null;
            default -> null;
        };
    }

    /** The reason of an @deprecated directive ("No longer supported" when it has none), or null when there is none. */
    static String deprecation(List<Directive> directives) {
        for (Directive d : directives)
            if (d.name().equals("deprecated"))
                return d.arguments().stream().filter(a -> a.name().equals("reason") && a.value() instanceof StringV)
                        .map(a -> ((StringV) a.value()).value()).findFirst().orElse("No longer supported");
        return null;
    }
}

package dev.shelfie.mini;

import dev.shelfie.mini.Ast.*;
import java.util.List;
import java.util.stream.Collectors;

/** Prints values (as introspection's defaultValue does) and a schema as SDL: the user's types, in definition order. */
public final class Printer {
    private Printer() {}

    public static String value(Value v) {
        return switch (v) {
            case Variable x -> "$" + x.name();
            case IntV i -> i.text();
            case FloatV f -> f.text();
            case StringV s -> string(s.value());
            case BooleanV b -> Boolean.toString(b.value());
            case NullV n -> "null";
            case EnumV e -> e.name();
            case ListV l -> l.values().stream().map(Printer::value).collect(Collectors.joining(", ", "[", "]"));
            case ObjectV o -> o.fields().stream().map(f -> f.name() + ": " + value(f.value())).collect(Collectors.joining(", ", "{", "}"));
        };
    }

    static String string(String s) {
        StringBuilder sb = new StringBuilder("\"");
        s.codePoints().forEach(c -> {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> { if (c < 0x20) sb.append(String.format("\\u%04X", c)); else sb.appendCodePoint(c); }
            }
        });
        return sb.append('"').toString();
    }

    public static String schema(Schema schema) {
        StringBuilder sb = new StringBuilder();
        boolean defaultRoots = "Query".equals(schema.query) && (schema.mutation == null || schema.mutation.equals("Mutation"))
                && (schema.subscription == null || schema.subscription.equals("Subscription"));
        if (!defaultRoots || schema.description != null) {
            sb.append(description(schema.description, "")).append("schema {\n");
            for (String op : List.of("query", "mutation", "subscription"))
                if (schema.root(op) != null) sb.append("  ").append(op).append(": ").append(schema.root(op)).append('\n');
            sb.append("}\n\n");
        }
        for (DirectiveDef d : schema.directives.values()) {
            if (Introspection.BUILT_IN_DIRECTIVES.contains(d.name())) continue;
            sb.append(description(d.description(), "")).append("directive @").append(d.name()).append(arguments(d.arguments(), ""))
              .append(d.repeatable() ? " repeatable" : "").append(" on ").append(String.join(" | ", d.locations())).append("\n\n");
        }
        for (Definition d : schema.types.values()) {
            String name = Schema.name(d);
            if (name.startsWith("__") || Schema.SCALARS.contains(name)) continue;
            sb.append(switch (d) {
                case ScalarDef s -> description(s.description(), "") + "scalar " + name + directives(s.directives());
                case ObjectDef o -> description(o.description(), "") + "type " + name + implementing(o.interfaces()) + directives(o.directives()) + fields(o.fields());
                case InterfaceDef i -> description(i.description(), "") + "interface " + name + implementing(i.interfaces()) + directives(i.directives()) + fields(i.fields());
                case UnionDef u -> description(u.description(), "") + "union " + name + directives(u.directives()) + " = " + String.join(" | ", u.members());
                case EnumDef e -> description(e.description(), "") + "enum " + name + directives(e.directives()) + " {\n"
                        + e.values().stream().map(v -> description(v.description(), "  ") + "  " + v.name() + directives(v.directives()) + "\n").collect(Collectors.joining()) + "}";
                case InputDef i -> description(i.description(), "") + "input " + name + directives(i.directives()) + " {\n"
                        + i.fields().stream().map(f -> description(f.description(), "  ") + "  " + inputValue(f) + "\n").collect(Collectors.joining()) + "}";
                default -> "";
            }).append("\n\n");
        }
        return sb.toString().stripTrailing() + "\n";
    }

    private static String implementing(List<String> interfaces) { return interfaces.isEmpty() ? "" : " implements " + String.join(" & ", interfaces); }

    private static String fields(List<FieldDef> fields) {
        return fields.stream().map(f -> description(f.description(), "  ") + "  " + f.name() + arguments(f.arguments(), "  ")
                + ": " + Schema.print(f.type()) + directives(f.directives()) + "\n").collect(Collectors.joining("", " {\n", "}"));
    }

    private static String arguments(List<InputValueDef> args, String indent) {
        if (args.isEmpty()) return "";
        if (args.stream().noneMatch(a -> a.description() != null))
            return args.stream().map(Printer::inputValue).collect(Collectors.joining(", ", "(", ")"));
        return args.stream().map(a -> description(a.description(), indent + "  ") + indent + "  " + inputValue(a) + "\n")
                .collect(Collectors.joining("", "(\n", indent + ")"));
    }

    private static String inputValue(InputValueDef v) {
        return v.name() + ": " + Schema.print(v.type()) + (v.defaultValue() == null ? "" : " = " + value(v.defaultValue())) + directives(v.directives());
    }

    private static String directives(List<Directive> directives) {
        return directives.stream().map(d -> " @" + d.name() + (d.arguments().isEmpty() ? ""
                : d.arguments().stream().map(a -> a.name() + ": " + value(a.value())).collect(Collectors.joining(", ", "(", ")")))).collect(Collectors.joining());
    }

    /** A description: a quoted string on one line, a block string when it spans lines. */
    private static String description(String d, String indent) {
        if (d == null) return "";
        if (!d.contains("\n")) return indent + string(d) + "\n";
        return indent + "\"\"\"\n" + d.lines().map(l -> l.isEmpty() ? "\n" : indent + l.replace("\"\"\"", "\\\"\"\"") + "\n").collect(Collectors.joining()) + indent + "\"\"\"\n";
    }
}

package dev.shelfie.mini;

import dev.shelfie.mini.Ast.*;
import java.util.List;
import java.util.stream.Collectors;

/** Prints values (as introspection's defaultValue does; provided) and a schema as SDL (lab 17.6). */
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

    /**
     * Lab 17.6: the schema as SDL (spec 3): the user's types and directives (not the built-ins or introspection
     * types), descriptions, applied directives, default values, and a schema definition only when the root types
     * are not named Query, Mutation and Subscription.
     */
    public static String schema(Schema schema) {
        throw new UnsupportedOperationException("TODO lab 17.6: print the schema as SDL");
    }
}

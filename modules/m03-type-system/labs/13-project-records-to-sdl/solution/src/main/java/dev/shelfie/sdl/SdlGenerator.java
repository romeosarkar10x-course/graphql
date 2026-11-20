package dev.shelfie.sdl;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.jspecify.annotations.NonNull;

/**
 * Code-first in about 200 lines: Java records, enums and sealed interfaces in, SDL out.
 *
 * <ul>
 *   <li>record: object type; components become fields, in declaration order</li>
 *   <li>enum: enum type</li>
 *   <li>sealed interface with methods: interface type (its methods are the fields); without: union</li>
 *   <li>String, int/Integer, double/Double/float/Float, boolean/Boolean: built-in scalars;
 *       {@link Id} String: ID; anything else (long, Map, ...): an error</li>
 *   <li>List/Set/Collection: list; Optional: always nullable</li>
 *   <li>non-null: primitives, and anything annotated {@code @NonNull} (also on list elements)</li>
 *   <li>{@link Description}: the SDL description</li>
 * </ul>
 * Types are collected from the roots transitively and printed sorted by name.
 */
public final class SdlGenerator {
    private SdlGenerator() {}

    private static final Map<Class<?>, String> SCALARS = Map.of(
            String.class, "String", int.class, "Int", Integer.class, "Int", double.class, "Float",
            Double.class, "Float", float.class, "Float", Float.class, "Float", boolean.class, "Boolean",
            Boolean.class, "Boolean");

    public static String generate(Class<?>... roots) {
        Map<String, Class<?>> types = new TreeMap<>();
        Deque<Class<?>> todo = new ArrayDeque<>(Arrays.asList(roots));
        while (!todo.isEmpty()) {
            Class<?> c = todo.pop();
            if (types.putIfAbsent(c.getSimpleName(), c) != null) continue;
            for (Class<?> ref : references(c)) if (!types.containsKey(ref.getSimpleName())) todo.push(ref);
        }
        List<String> blocks = new ArrayList<>();
        for (Class<?> c : types.values()) blocks.add(render(c, types.values()));
        return String.join("\n", blocks);
    }

    // ---------------------------------------------------------------- discovery

    private static Set<Class<?>> references(Class<?> c) {
        Set<Class<?>> out = new LinkedHashSet<>();
        if (c.isRecord()) {
            for (RecordComponent rc : c.getRecordComponents()) collect(rc.getAnnotatedType(), out);
            for (Class<?> i : c.getInterfaces()) if (i.isSealed()) out.add(i);
        } else if (c.isSealed()) {
            out.addAll(Arrays.asList(c.getPermittedSubclasses()));
            for (Method m : fields(c)) collect(m.getAnnotatedReturnType(), out);
        }
        return out;
    }

    private static void collect(AnnotatedType t, Set<Class<?>> out) {
        Class<?> raw = raw(t.getType());
        if (Collection.class.isAssignableFrom(raw) || raw == Optional.class) {
            collect(element(t), out);
        } else if (raw.isRecord() || raw.isEnum() || (raw.isInterface() && raw.isSealed())) {
            out.add(raw);
        }
    }

    // ---------------------------------------------------------------- rendering

    private static String render(Class<?> c, Collection<Class<?>> all) {
        StringBuilder sb = new StringBuilder();
        description(c, "").ifPresent(sb::append);
        String name = c.getSimpleName();
        if (c.isEnum()) {
            sb.append("enum ").append(name).append(" {\n");
            for (Object constant : c.getEnumConstants()) {
                String v = ((Enum<?>) constant).name();
                try {
                    description(c.getField(v), "  ").ifPresent(sb::append);
                } catch (NoSuchFieldException e) {
                    throw new IllegalStateException(e);
                }
                sb.append("  ").append(v).append("\n");
            }
            return sb.append("}\n").toString();
        }
        if (c.isInterface() && fields(c).isEmpty()) {
            List<String> members = Arrays.stream(c.getPermittedSubclasses()).map(Class::getSimpleName).sorted().toList();
            return sb.append("union ").append(name).append(" = ").append(String.join(" | ", members)).append("\n").toString();
        }
        sb.append(c.isInterface() ? "interface " : "type ").append(name);
        List<String> interfaces = Arrays.stream(c.getInterfaces())
                .filter(i -> i.isSealed() && all.contains(i) && !fields(i).isEmpty())
                .map(Class::getSimpleName).sorted().toList();
        if (!interfaces.isEmpty()) sb.append(" implements ").append(String.join(" & ", interfaces));
        sb.append(" {\n");
        if (c.isRecord()) {
            for (RecordComponent rc : c.getRecordComponents()) {
                field(sb, c, rc.getName(), rc.getAnnotatedType(), rc);
            }
        } else {
            for (Method m : fields(c)) field(sb, c, m.getName(), m.getAnnotatedReturnType(), m);
        }
        return sb.append("}\n").toString();
    }

    private static void field(StringBuilder sb, Class<?> owner, String name, AnnotatedType type, AnnotatedElement element) {
        description(element, "  ").ifPresent(sb::append);
        String where = owner.getSimpleName() + "." + name;
        sb.append("  ").append(name).append(": ").append(typeRef(type, element.isAnnotationPresent(Id.class), where)).append("\n");
    }

    /** The GraphQL type reference for a Java type, e.g. [User!]! */
    private static String typeRef(AnnotatedType t, boolean id, String where) {
        Class<?> raw = raw(t.getType());
        boolean nonNull = raw.isPrimitive() || t.isAnnotationPresent(NonNull.class);
        String inner;
        if (raw == Optional.class) {
            String elem = typeRef(element(t), id, where);
            return elem.endsWith("!") ? elem.substring(0, elem.length() - 1) : elem;
        } else if (Collection.class.isAssignableFrom(raw)) {
            inner = "[" + typeRef(element(t), id, where) + "]";
        } else if (id) {
            if (raw != String.class) throw new IllegalArgumentException(where + ": @Id must be a String");
            inner = "ID";
        } else if (SCALARS.containsKey(raw)) {
            inner = SCALARS.get(raw);
        } else if (raw.isRecord() || raw.isEnum() || (raw.isInterface() && raw.isSealed())) {
            inner = raw.getSimpleName();
        } else {
            throw new IllegalArgumentException(where + ": no GraphQL type for " + raw.getName()
                    + (raw == long.class || raw == Long.class ? " (Int is 32-bit; use an ID or a custom scalar, lab 3.2)" : ""));
        }
        return nonNull ? inner + "!" : inner;
    }

    // ---------------------------------------------------------------- reflection helpers

    /** Abstract, no-argument methods of a sealed interface: its GraphQL fields, sorted by name. */
    private static List<Method> fields(Class<?> c) {
        if (!c.isInterface()) return List.of();
        return Arrays.stream(c.getDeclaredMethods())
                .filter(m -> Modifier.isAbstract(m.getModifiers()) && m.getParameterCount() == 0)
                .sorted(Comparator.comparing(Method::getName)).toList();
    }

    private static Class<?> raw(Type t) {
        if (t instanceof Class<?> c) return c;
        if (t instanceof ParameterizedType p) return (Class<?>) p.getRawType();
        throw new IllegalArgumentException("unsupported type " + t);
    }

    private static AnnotatedType element(AnnotatedType t) {
        if (t instanceof AnnotatedParameterizedType p) return p.getAnnotatedActualTypeArguments()[0];
        throw new IllegalArgumentException("raw collection type " + t.getType() + ": add a type argument");
    }

    private static Optional<String> description(AnnotatedElement e, String indent) {
        Description d = e.getAnnotation(Description.class);
        return d == null ? Optional.empty() : Optional.of(indent + "\"" + d.value().replace("\"", "\\\"") + "\"\n");
    }
}

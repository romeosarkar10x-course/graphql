import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Binds a GraphQL input value (what graphql-java hands a data fetcher: maps, lists, Strings, Integers,
 * Booleans) to a Java record, the way Spring for GraphQL's @Argument does (Module 6).
 */
final class Bind {
    private Bind() {}

    static <T extends Record> T bind(Map<String, ?> value, Class<T> type) {
        RecordComponent[] cs = type.getRecordComponents();
        Object[] args = new Object[cs.length];
        Class<?>[] types = new Class<?>[cs.length];
        for (int i = 0; i < cs.length; i++) {
            RecordComponent c = cs[i];
            types[i] = c.getType();
            String where = type.getSimpleName() + "." + c.getName();
            if (c.getType() == Optional.class) {
                // absent -> null; present but null -> Optional.empty(); present -> Optional.of(v)
                args[i] = !value.containsKey(c.getName()) ? null
                        : Optional.ofNullable(convert(value.get(c.getName()), arg(c.getGenericType()), where));
            } else {
                Object v = value.get(c.getName());
                if (v == null && c.getType().isPrimitive()) throw new IllegalArgumentException(where + " is required");
                args[i] = convert(v, c.getGenericType(), where);
            }
        }
        try {
            var ctor = type.getDeclaredConstructor(types);
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convert(Object v, Type target, String where) {
        if (v == null) return null;
        Class<?> raw = target instanceof ParameterizedType p ? (Class<?>) p.getRawType() : (Class<?>) target;
        if (raw == String.class) return v.toString();
        if (raw == int.class || raw == Integer.class) return ((Number) v).intValue();
        if (raw == boolean.class || raw == Boolean.class) return (Boolean) v;
        if (raw.isEnum()) return Enum.valueOf((Class<Enum>) raw, v.toString());
        if (raw.isRecord()) return bind((Map<String, ?>) v, (Class<Record>) raw);
        if (raw == List.class) {
            List<Object> out = new ArrayList<>();
            for (Object item : (List<?>) v) out.add(convert(item, arg(target), where + "[]"));
            return List.copyOf(out);
        }
        throw new IllegalArgumentException(where + ": cannot bind " + raw.getName());
    }

    private static Type arg(Type t) {
        return ((ParameterizedType) t).getActualTypeArguments()[0];
    }
}

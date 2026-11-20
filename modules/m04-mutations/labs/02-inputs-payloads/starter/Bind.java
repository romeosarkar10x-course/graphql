import java.lang.reflect.RecordComponent;
import java.util.Map;

/**
 * Binds a GraphQL input value (what graphql-java hands a data fetcher: maps, lists, Strings, Integers,
 * Booleans) to a Java record, the way Spring for GraphQL's @Argument does (Module 6).
 */
final class Bind {
    private Bind() {}

    // TODO: enums, nested records, lists, Optional (three states), and a clear error when a primitive
    // component has no value. This draft handles flat records of Strings and ints only.
    static <T extends Record> T bind(Map<String, ?> value, Class<T> type) {
        RecordComponent[] cs = type.getRecordComponents();
        Object[] args = new Object[cs.length];
        Class<?>[] types = new Class<?>[cs.length];
        for (int i = 0; i < cs.length; i++) {
            types[i] = cs[i].getType();
            Object v = value.get(cs[i].getName());
            args[i] = cs[i].getType() == int.class ? (v == null ? 0 : ((Number) v).intValue()) : v;
        }
        try {
            var ctor = type.getDeclaredConstructor(types);
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}

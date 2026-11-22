package dev.shelfie.mini;

import java.lang.reflect.RecordComponent;
import java.util.Map;

/**
 * Where field values come from: the spec's ResolveFieldValue (6.4.2) and ResolveAbstractType (6.4.3), as one
 * interface the engine calls. Arguments arrive coerced: Integer, Double, String, Boolean, null, List and Map; enum
 * values as their names.
 */
public interface Resolver {
    Object resolve(String parentType, String field, Object source, Map<String, Object> arguments) throws Exception;

    /** The object type of a value returned for an interface or union field. */
    default String typeOf(Object value) {
        if (value instanceof Map<?, ?> m && m.get("__typename") instanceof String t) return t;
        return value.getClass().getSimpleName();
    }

    /** The default resolver: a map's entry, a record's component, or null. */
    Resolver PROPERTIES = (type, field, source, args) -> property(source, field);

    static Object property(Object source, String name) throws Exception {
        if (source instanceof Map<?, ?> m) return m.get(name);
        if (source != null && source.getClass().isRecord())
            for (RecordComponent c : source.getClass().getRecordComponents())
                if (c.getName().equals(name)) return c.getAccessor().invoke(source);
        return null;
    }
}

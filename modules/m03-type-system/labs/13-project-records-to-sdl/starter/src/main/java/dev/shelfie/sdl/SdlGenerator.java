package dev.shelfie.sdl;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Code-first in about 200 lines: Java records, enums and sealed interfaces in, SDL out.
 * The rules are in the lab README. This first draft handles one record and its scalar components.
 */
public final class SdlGenerator {
    private SdlGenerator() {}

    private static final Map<Class<?>, String> SCALARS = Map.of(String.class, "String", int.class, "Int");

    public static String generate(Class<?>... roots) {
        List<String> blocks = new ArrayList<>();
        // TODO: collect every type reachable from the roots (components, list elements, Optional,
        // sealed interfaces and their permitted subclasses), then render each one, sorted by name.
        for (Class<?> c : roots) blocks.add(render(c));
        return String.join("\n", blocks);
    }

    private static String render(Class<?> c) {
        // TODO: enums, unions (sealed, no methods), interfaces (sealed, with methods), implements,
        // descriptions, @Id, lists, Optional, nullability (@NonNull and primitives), errors for long.
        StringBuilder sb = new StringBuilder("type ").append(c.getSimpleName()).append(" {\n");
        for (RecordComponent rc : c.getRecordComponents()) {
            String t = SCALARS.getOrDefault(rc.getType(), rc.getType().getSimpleName());
            sb.append("  ").append(rc.getName()).append(": ").append(t).append("\n");
        }
        return sb.append("}\n").toString();
    }
}

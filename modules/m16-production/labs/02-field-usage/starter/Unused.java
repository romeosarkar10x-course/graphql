import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Which fields did no client use? Reads the usage reports a registry holds and the schema, prints every field
 * coordinate (Type.field) that no operation in the window selected, one per line, sorted.
 *
 *   bin/course java Unused.java <schema.graphqls> [--reports http://localhost:4200/_reports]
 *                               [--now 2026-11-15T00:00:00Z] [--days 7]
 *
 * The window is [now - days, now). An operation counts by its own timestamp, not by when it was reported.
 * graphql-java and Jackson 3 are on the class path (bin/course java).
 */
public class Unused {
    public static void main(String[] a) throws Exception {
        Path schema = Path.of(a[0]);
        // TODO 16.2
    }
}

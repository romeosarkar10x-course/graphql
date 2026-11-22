import graphql.schema.diff.DiffLevel;
import graphql.schema.diff.SchemaDiff;
import graphql.schema.diff.SchemaDiffSet;
import graphql.schema.diff.reporting.CapturingReporter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The schema gate for CI (lab 13.5). Today it prints graphql-java's diff and always passes.
 *
 *   bin/course java SchemaCheck.java <old.graphqls> <new.graphqls> <clients-dir>
 *
 * TODO the output format in the README: one line per change with its schema coordinate and why it matters to
 * a client, the operations it touches ("used by ..."), every client operation that no longer validates
 * ("INVALID ..."), a verdict line, and exit 1 only when a known client is hurt.
 */
public class SchemaCheck {
    public static void main(String[] args) throws Exception {
        String oldSdl = Files.readString(Path.of(args[0])), newSdl = Files.readString(Path.of(args[1]));
        var reporter = new CapturingReporter();
        new SchemaDiff().diffSchema(SchemaDiffSet.diffSetFromSdl(oldSdl, newSdl), reporter);
        reporter.getEvents().stream().filter(e -> e.getLevel() != DiffLevel.INFO)
            .forEach(e -> System.out.println(e.getLevel() + " " + e.getReasonMsg()));
    }
}

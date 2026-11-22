import graphql.schema.diff.DiffLevel;
import graphql.schema.diff.SchemaDiff;
import graphql.schema.diff.SchemaDiffSet;
import graphql.schema.diff.reporting.CapturingReporter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Game 13.9's referee: graphql-java's SchemaDiff verdict for every round, "NN:breaking|dangerous|safe".
 *   bin/course java RoundVerdicts.java <old.graphqls> <rounds-dir>      (each round is <rounds-dir>/NN/new.graphqls)
 */
public class RoundVerdicts {
    public static void main(String[] args) throws Exception {
        String old = Files.readString(Path.of(args[0]));
        try (var rounds = Files.list(Path.of(args[1]))) {
            for (Path r : rounds.sorted().toList()) {
                var reporter = new CapturingReporter();
                new SchemaDiff().diffSchema(SchemaDiffSet.diffSetFromSdl(old, Files.readString(r.resolve("new.graphqls"))), reporter);
                String verdict = reporter.getBreakageCount() > 0 ? "breaking" : reporter.getDangerCount() > 0 ? "dangerous" : "safe";
                System.out.println(r.getFileName() + ":" + verdict);
            }
        }
    }
}

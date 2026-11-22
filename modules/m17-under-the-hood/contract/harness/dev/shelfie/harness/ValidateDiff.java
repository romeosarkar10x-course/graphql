package dev.shelfie.harness;

import dev.shelfie.mini.Parser;
import dev.shelfie.mini.Resolver;
import dev.shelfie.mini.Schema;
import dev.shelfie.mini.Validator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Lab 17.4: your six rules against graphql-java's on every .graphql file in the given directories, over the
 * reference schema. Per document and rule, prints "ok" or what differs, then a summary line
 * "documents A of N agree; errors yours X graphql-java Y". Counts must be equal for four rules. For
 * NoFragmentCycles and OverlappingFieldsCanBeMerged the spec does not say how many errors to report, so only
 * whether the rule fires is compared. --all also lists rules outside the six that graphql-java reports (authoring check).
 */
public final class ValidateDiff {
    static final Set<String> FIRES_ONLY = Set.of("NoFragmentCycles", "OverlappingFieldsCanBeMerged");

    public static void main(String[] a) throws Exception {
        String sdl = Files.readString(Path.of("/course/reference/shelfie-ref/src/main/resources/graphql/schema.graphqls"));
        Schema mini = new Schema(sdl);
        var gj = Gj.schema(sdl, Resolver.PROPERTIES);
        boolean all = List.of(a).contains("--all");
        int docs = 0, agree = 0, mineTotal = 0, theirTotal = 0;
        for (String dir : a) {
            if (dir.startsWith("--")) continue;
            List<Path> files;
            try (Stream<Path> s = Files.list(Path.of(dir))) { files = s.filter(p -> p.toString().endsWith(".graphql")).sorted().toList(); }
            for (Path f : files) {
                String q = Files.readString(f);
                docs++;
                Map<String, Integer> mine = new TreeMap<>();
                String crash = null;
                try {
                    for (Validator.Problem p : Validator.validate(mini, Parser.parse(q))) mine.merge(p.rule(), 1, Integer::sum);
                } catch (RuntimeException e) { crash = e.toString(); }
                Map<String, Integer> theirs = new TreeMap<>(Gj.validate(gj, q));
                mineTotal += mine.values().stream().mapToInt(Integer::intValue).sum();
                theirTotal += theirs.values().stream().mapToInt(Integer::intValue).sum();
                StringBuilder diff = new StringBuilder();
                if (crash != null) diff.append(" crashed: ").append(crash);
                for (String rule : Gj.RULES.keySet()) {
                    int x = mine.getOrDefault(rule, 0), y = theirs.getOrDefault(rule, 0);
                    boolean same = FIRES_ONLY.contains(rule) ? (x > 0) == (y > 0) : x == y;
                    if (!same) diff.append(" ").append(rule).append(": yours ").append(x).append(", graphql-java ").append(y).append(';');
                }
                if (diff.isEmpty()) agree++;
                String line = (diff.isEmpty() ? "ok    " : "DIFF  ") + f.getFileName() + "  " + (diff.isEmpty() ? theirs : diff.toString().trim());
                if (all) {
                    var every = new graphql.validation.Validator().validateDocument(gj, Gj.parse(q), Locale.ENGLISH);
                    var extra = every.stream().map(e -> e.getValidationErrorType().toString()).filter(t -> !Set.of("FieldUndefined", "UnknownArgument",
                            "UnusedVariable", "FragmentCycle", "VariableTypeMismatch", "FieldsConflict").contains(t)).distinct().toList();
                    if (!extra.isEmpty()) line += "   ALSO " + extra;
                }
                System.out.println(line);
            }
        }
        System.out.println("documents " + agree + " of " + docs + " agree; errors yours " + mineTotal + " graphql-java " + theirTotal);
    }
}

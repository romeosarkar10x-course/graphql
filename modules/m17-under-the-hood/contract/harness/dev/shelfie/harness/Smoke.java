package dev.shelfie.harness;

import dev.shelfie.mini.MiniGraphQL;
import dev.shelfie.mini.Schema;
import java.nio.file.Files;
import java.nio.file.Path;
import tools.jackson.databind.ObjectMapper;

/** Runs every .graphql file given on both engines over the reference data and prints whether they agree. */
public class Smoke {
    public static void main(String[] args) throws Exception {
        String sdl = Files.readString(Path.of("/course/reference/shelfie-ref/src/main/resources/graphql/schema.graphqls"));
        Schema mini = new Schema(sdl);
        ShelfieResolver r = new ShelfieResolver();
        var gj = Gj.schema(sdl, r);
        ObjectMapper json = new ObjectMapper();
        for (String f : args) {
            String q = Files.readString(Path.of(f));
            Path varsFile = Path.of(f.replace(".graphql", ".json"));
            java.util.Map<String, Object> vars = Files.exists(varsFile) ? json.readValue(varsFile.toFile(), java.util.Map.class) : null;
            String a = json.writeValueAsString(MiniGraphQL.execute(mini, q, null, vars, r, null));
            String b = json.writeValueAsString(Gj.execute(gj, q, null, vars, null));
            System.out.println((json.readTree(a).equals(json.readTree(b)) ? "same " : "DIFF ") + Path.of(f).getFileName());
            if (!json.readTree(a).equals(json.readTree(b))) { System.out.println("  mini: " + a.substring(0, Math.min(400, a.length()))); System.out.println("  gj:   " + b.substring(0, Math.min(400, b.length()))); }
        }
    }
}

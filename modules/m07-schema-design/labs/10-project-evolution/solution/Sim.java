import graphql.ExecutionInput;
import graphql.ParseAndValidate;
import graphql.language.*;
import graphql.parser.Parser;
import graphql.schema.*;
import graphql.schema.diff.SchemaDiff;
import graphql.schema.diff.SchemaDiffSet;
import graphql.schema.diff.reporting.CapturingReporter;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.UnExecutableSchemaGenerator;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Mini-project 7: the evolution simulator. Run from the lab directory:
 *
 *   ../../../bin/course java Sim.java          check every step you have shipped; reveal the next change request
 *   ../../../bin/course java Sim.java check    the same, but reveal nothing (the test uses this)
 *
 * Step N is steps/0N.graphqls: the whole schema after change request N. Each step must keep every
 * deployed client valid, pass graphql-java's SchemaDiff with nothing BREAKING, deprecate what it
 * replaces, and stay valid with its deprecated elements removed (RFC #1231). The change requests are
 * encoded below so they arrive one at a time; reading ahead spoils the game, not the tests.
 */
public class Sim {
    static final String DATA = "eyJ2MCI6ICIjIFNoZWxmaWUsIHJlbGVhc2UgMC4gU2l4IGNoYW5nZSByZXF1ZXN0cyB3aWxsIGFycml2ZSBvbmUgYXQgYSB0aW1lIChSRUFETUUpLiBOZXZlciBlZGl0IHRoaXMgZmlsZTpcbiMgZWFjaCBzdGVwIGlzIGEgbmV3IGZpbGUgaW4gc3RlcHMvLlxudHlwZSBRdWVyeSB7XG4gIGNsdWIoaWQ6IElEISk6IENsdWJcbiAgY2x1YnM6IFtDbHViIV0hXG4gIGJvb2soaWQ6IElEISk6IEJvb2tcbn1cblxudHlwZSBDbHViIHtcbiAgaWQ6IElEIVxuICBuYW1lOiBTdHJpbmchXG4gIFwiUGxhaW4gdGV4dC5cIlxuICBkZXNjcmlwdGlvbjogU3RyaW5nXG4gIG93bmVyOiBVc2VyIVxuICBjdXJyZW50Qm9vazogQm9va1xuICBtZW1iZXJzOiBbVXNlciFdIVxufVxuXG50eXBlIFVzZXIge1xuICBpZDogSUQhXG4gIG5hbWU6IFN0cmluZyFcbiAgaGFuZGxlOiBTdHJpbmchXG59XG5cbnR5cGUgQm9vayB7XG4gIGlkOiBJRCFcbiAgdGl0bGU6IFN0cmluZyFcbiAgXCJUaGUgYXV0aG9yLCBhcyBwcmludGVkLiBPbmUgbmFtZS5cIlxuICBhdXRob3I6IFN0cmluZyFcbiAgcmV2aWV3czogW1JldmlldyFdIVxufVxuXG50eXBlIFJldmlldyB7XG4gIGlkOiBJRCFcbiAgXCIxIHRvIDUgc3RhcnMuXCJcbiAgcmF0aW5nOiBJbnQhXG4gIGJvZHk6IFN0cmluZ1xuICBhdXRob3I6IFVzZXIhXG59XG4iLCAidjBDbGllbnRzIjogeyJib29rLXBhZ2UuZ3JhcGhxbCI6ICJxdWVyeSBCb29rUGFnZSgkaWQ6IElEISkgeyBib29rKGlkOiAkaWQpIHsgaWQgdGl0bGUgYXV0aG9yIHJldmlld3MgeyBpZCByYXRpbmcgYm9keSBhdXRob3IgeyBuYW1lIH0gfSB9IH1cbiIsICJjbHViLWRpcmVjdG9yeS5ncmFwaHFsIjogInF1ZXJ5IENsdWJEaXJlY3RvcnkgeyBjbHVicyB7IGlkIG5hbWUgZGVzY3JpcHRpb24gfSB9XG4iLCAiY2x1Yi1wYWdlLmdyYXBocWwiOiAicXVlcnkgQ2x1YlBhZ2UoJGlkOiBJRCEpIHsgY2x1YihpZDogJGlkKSB7IGlkIG5hbWUgZGVzY3JpcHRpb24gb3duZXIgeyBuYW1lIH0gY3VycmVudEJvb2sgeyBpZCB0aXRsZSBhdXRob3IgfSB9IH1cbiIsICJtZW1iZXItbGlzdC5ncmFwaHFsIjogInF1ZXJ5IE1lbWJlckxpc3QoJGlkOiBJRCEpIHsgY2x1YihpZDogJGlkKSB7IGlkIG1lbWJlcnMgeyBpZCBuYW1lIGhhbmRsZSB9IH0gfVxuIiwgInJldmlldy1zdHJpcC5ncmFwaHFsIjogInF1ZXJ5IFJldmlld1N0cmlwKCRpZDogSUQhKSB7IGJvb2soaWQ6ICRpZCkgeyBpZCByZXZpZXdzIHsgaWQgcmF0aW5nIH0gfSB9XG4ifSwgInYxQ2xpZW50cyI6IHsiYm9vay1wYWdlLmdyYXBocWwiOiAicXVlcnkgQm9va1BhZ2UoJGlkOiBJRCEpIHsgYm9vayhpZDogJGlkKSB7IGlkIHRpdGxlIGF1dGhvcnMgeyBpZCBuYW1lIH0gcmV2aWV3cyB7IGlkIHNjb3JlIGJvZHkgYXV0aG9yIHsgbmFtZSB9IH0gfSB9XG4iLCAiY2x1Yi1kaXJlY3RvcnkuZ3JhcGhxbCI6ICJxdWVyeSBDbHViRGlyZWN0b3J5IHsgY2x1YnMgeyBpZCBuYW1lIGFib3V0IH0gfVxuIiwgImNsdWItcGFnZS5ncmFwaHFsIjogInF1ZXJ5IENsdWJQYWdlKCRpZDogSUQhKSB7IGNsdWIoaWQ6ICRpZCkgeyBpZCBuYW1lIGFib3V0IG93bmVyIHsgbmFtZSB9IGN1cnJlbnRCb29rIHsgaWQgdGl0bGUgYXV0aG9ycyB7IGlkIG5hbWUgfSB9IH0gfVxuIiwgIm1lbWJlci1saXN0LmdyYXBocWwiOiAicXVlcnkgTWVtYmVyTGlzdCgkaWQ6IElEISkgeyBjbHViKGlkOiAkaWQpIHsgaWQgbWVtYmVyc2hpcHMgeyByb2xlIGpvaW5lZEF0IHVzZXIgeyBpZCBuYW1lIGhhbmRsZSB9IH0gfSB9XG4iLCAicmV2aWV3LXN0cmlwLmdyYXBocWwiOiAiIyBSZWxlYXNlIDEga2VwdCB0aGUgd2hvbGUtc3RhciBzdHJpcDogcmF0aW5nIHN0YXlzIGluIHVzZS5cbnF1ZXJ5IFJldmlld1N0cmlwKCRpZDogSUQhKSB7IGJvb2soaWQ6ICRpZCkgeyBpZCByZXZpZXdzIHsgaWQgcmF0aW5nIH0gfSB9XG4iLCAic2VhcmNoLmdyYXBocWwiOiAicXVlcnkgU2VhcmNoKCR0ZXh0OiBTdHJpbmchKSB7IHNlYXJjaCh0ZXh0OiAkdGV4dCkgeyBfX3R5cGVuYW1lIC4uLiBvbiBCb29rIHsgaWQgdGl0bGUgYXV0aG9ycyB7IG5hbWUgfSB9IC4uLiBvbiBDbHViIHsgaWQgbmFtZSB9IH0gfVxuIn0sICJjciI6IFt7InRleHQiOiAiIyBDaGFuZ2UgcmVxdWVzdCAxOiBkZXNjcmlwdGlvbnMgaW4gTWFya2Rvd25cblxuQ2x1YiBvd25lcnMgd2FudCBib2xkLCBsaW5rcyBhbmQgbGlzdHMgaW4gdGhlaXIgY2x1YiBkZXNjcmlwdGlvbi4gUHJvZHVjdDogXCJBZGQgYSBNYXJrZG93blxuZGVzY3JpcHRpb24uIFRoZSBvbGQgcGxhaW4tdGV4dCBvbmUgbXVzdCBrZWVwIHdvcmtpbmc6IHJlbGVhc2UtMCBhcHBzIHNob3cgaXQuXCJcblxuQWNjZXB0ZWQgd2hlbiB0aGUgbmV3IGNsaWVudCB3b3JrczogYGNyMDEtY2x1Yi1hYm91dC5ncmFwaHFsYC5cbiIsICJjbGllbnRzIjogeyJjcjAxLWNsdWItYWJvdXQuZ3JhcGhxbCI6ICJxdWVyeSBDbHViQWJvdXQoJGlkOiBJRCEpIHsgY2x1YihpZDogJGlkKSB7IGlkIGFib3V0IH0gfVxuIn19LCB7InRleHQiOiAiIyBDaGFuZ2UgcmVxdWVzdCAyOiByb2xlc1xuXG5Nb2RlcmF0b3JzIGV4aXN0IG5vdy4gVGhlIG1lbWJlciBsaXN0IG11c3Qgc2hvdyBlYWNoIG1lbWJlcidzIHJvbGUgaW4gdGhlIGNsdWIgKGBPV05FUmAsXG5gTU9ERVJBVE9SYCwgYE1FTUJFUmApIGFuZCB3aGVuIHRoZXkgam9pbmVkLiBBIHBlcnNvbiBjYW4gYmUgYSBtb2RlcmF0b3IgaW4gb25lIGNsdWIgYW5kIGEgbWVtYmVyIGluXG5hbm90aGVyLlxuXG5BY2NlcHRlZCB3aGVuOiBgY3IwMi1tZW1iZXItcm9sZXMuZ3JhcGhxbGAuXG4iLCAiY2xpZW50cyI6IHsiY3IwMi1tZW1iZXItcm9sZXMuZ3JhcGhxbCI6ICJxdWVyeSBNZW1iZXJSb2xlcygkaWQ6IElEISkgeyBjbHViKGlkOiAkaWQpIHsgaWQgbWVtYmVyc2hpcHMgeyByb2xlIGpvaW5lZEF0IHVzZXIgeyBpZCBuYW1lIH0gfSB9IH1cbiJ9fSwgeyJ0ZXh0IjogIiMgQ2hhbmdlIHJlcXVlc3QgMzogbW9yZSB0aGFuIG9uZSBhdXRob3JcblxuXCJHb29kIE9tZW5zXCIgaGFzIHR3byBhdXRob3JzLCBhbmQgYW50aG9sb2dpZXMgaGF2ZSB0ZW4uIEJvb2tzIG5lZWQgYSBsaXN0IG9mIGF1dGhvcnMsIGVhY2ggYSBwZXJzb25cbndpdGggYSBuYW1lIChhdXRob3JzIGdldCB0aGVpciBvd24gcGFnZXMgbGF0ZXIpLlxuXG5BY2NlcHRlZCB3aGVuOiBgY3IwMy1ib29rLWF1dGhvcnMuZ3JhcGhxbGAuXG4iLCAiY2xpZW50cyI6IHsiY3IwMy1ib29rLWF1dGhvcnMuZ3JhcGhxbCI6ICJxdWVyeSBCb29rQXV0aG9ycygkaWQ6IElEISkgeyBib29rKGlkOiAkaWQpIHsgaWQgYXV0aG9ycyB7IGlkIG5hbWUgfSB9IH1cbiJ9fSwgeyJ0ZXh0IjogIiMgQ2hhbmdlIHJlcXVlc3QgNDogaGFsZiBzdGFyc1xuXG5SZXZpZXdlcnMgd2FudCB0byBnaXZlIDMuNSBzdGFycy4gUmF0aW5ncyBiZWNvbWUgYSBkZWNpbWFsIGZyb20gMC41IHRvIDUuMCBpbiBzdGVwcyBvZiAwLjUuIE9sZCBhcHBzXG5zaG93IHdob2xlIHN0YXJzIGFuZCBtdXN0IGtlZXAgd29ya2luZyAocm91bmQgaGFsZiB1cCBmb3IgdGhlbSkuXG5cbkFjY2VwdGVkIHdoZW46IGBjcjA0LXJldmlldy1zY29yZS5ncmFwaHFsYC5cbiIsICJjbGllbnRzIjogeyJjcjA0LXJldmlldy1zY29yZS5ncmFwaHFsIjogInF1ZXJ5IFJldmlld1Njb3JlKCRpZDogSUQhKSB7IGJvb2soaWQ6ICRpZCkgeyBpZCByZXZpZXdzIHsgaWQgc2NvcmUgfSB9IH1cbiJ9fSwgeyJ0ZXh0IjogIiMgQ2hhbmdlIHJlcXVlc3QgNTogc2VhcmNoXG5cbk9uZSBzZWFyY2ggYm94IGZvciBib29rcyBhbmQgY2x1YnMuIFJlc3VsdHMgY29tZSBiYWNrIG1peGVkLCBib29rcyBhbmQgY2x1YnMgaW4gb25lIGxpc3QsIGJlc3QgZmlyc3QuXG5cbkFjY2VwdGVkIHdoZW46IGBjcjA1LXNlYXJjaC5ncmFwaHFsYC5cbiIsICJjbGllbnRzIjogeyJjcjA1LXNlYXJjaC5ncmFwaHFsIjogInF1ZXJ5IFNlYXJjaCB7IHNlYXJjaCh0ZXh0OiBcImRhcmtcIikgeyBfX3R5cGVuYW1lIC4uLiBvbiBCb29rIHsgaWQgdGl0bGUgfSAuLi4gb24gQ2x1YiB7IGlkIG5hbWUgfSB9IH1cbiJ9fSwgeyJ0ZXh0IjogIiMgQ2hhbmdlIHJlcXVlc3QgNjogcmVsZWFzZSAwIGlzIHJldGlyZWRcblxuVGhlIHJlbGVhc2UtMCBhcHBzIGFyZSBnb25lOiB0aGUgc3RvcmUgZm9yY2VzIGFuIHVwZGF0ZSBhbmQgdXNhZ2UgbG9ncyBzaG93IHplcm8gcmVsZWFzZS0wIHRyYWZmaWMgZm9yXG45MCBkYXlzLiBSZWxlYXNlLTEgYXBwcyAodGhlaXIgb3BlcmF0aW9ucyBhcmUgaW4gYHYxL2ApIGFyZSB3aGF0J3MgaW5zdGFsbGVkIG5vdy5cblxuUHJvZHVjdDogXCJEZWxldGUgd2hhdCBub2JvZHkgdXNlcyBhbnkgbW9yZS5cIiBVc2FnZSBsb2dzIGZvciByZWxlYXNlIDEgc2F5OiBgQ2x1Yi5kZXNjcmlwdGlvbmAgYW5kXG5gQm9vay5hdXRob3JgIGhhdmUgbm8gdHJhZmZpYy4gRXZlcnl0aGluZyBhIHJlbGVhc2UtMSBvcGVyYXRpb24gc2VsZWN0cyBtdXN0IHN0YXkuXG5cbkFjY2VwdGVkIHdoZW46IGV2ZXJ5IGB2MS9gIG9wZXJhdGlvbiBpcyB2YWxpZCwgYW5kIGBDbHViLmRlc2NyaXB0aW9uYCBhbmQgYEJvb2suYXV0aG9yYCBhcmUgZ29uZS5cbiIsICJjbGllbnRzIjoge319XX0=";
    static JsonNode data;
    static int fails;

    public static void main(String[] args) throws Exception {
        data = JsonMapper.builder().build().readTree(new String(Base64.getDecoder().decode(DATA), StandardCharsets.UTF_8));
        boolean reveal = !(args.length > 0 && args[0].equals("check"));
        String prev = data.get("v0").asString();
        for (int n = 1; n <= 6; n++) {
            Path step = Path.of(String.format("steps/%02d.graphqls", n));
            if (!Files.exists(step)) {
                if (reveal) revealRequest(n);
                else System.out.println("missing " + step);
                System.exit(reveal ? 0 : 1);
            }
            String sdl = Files.readString(step);
            System.out.println("== step " + n);
            fails = 0;
            check(n, prev, sdl);
            if (fails > 0) {
                System.out.println("step " + n + ": " + fails + " problem(s). Fix steps/" + String.format("%02d", n) + ".graphqls and run again.");
                System.exit(1);
            }
            System.out.println("step " + n + " shipped: no deployed client broke");
            prev = sdl;
        }
        System.out.println("all 6 change requests shipped with zero client breakage");
    }

    static void ok(boolean good, String what) {
        System.out.println((good ? "  ok   " : "  fail ") + what);
        if (!good) fails++;
    }

    static void check(int n, String prev, String sdl) {
        GraphQLSchema s;
        try { s = UnExecutableSchemaGenerator.makeUnExecutableSchema(new SchemaParser().parse(sdl)); }
        catch (Exception e) { ok(false, "the schema builds: " + String.valueOf(e.getMessage()).lines().findFirst().orElse("")); return; }
        // Deployed clients
        String fleet = n < 6 ? "v0" : "v1";
        data.get(fleet + "Clients").properties().forEach(c -> ok(valid(s, c.getValue().asString()),
            "deployed " + fleet + " client " + c.getKey() + " still valid"));
        // Every change request so far is still delivered
        for (int k = 1; k <= n; k++)
            for (var c : data.get("cr").get(k - 1).get("clients").properties())
                ok(valid(s, c.getValue().asString()), "change request " + k + " delivered: " + c.getKey() + " is valid");
        // SchemaDiff against the previous step
        var r = new CapturingReporter();
        new SchemaDiff().diffSchema(SchemaDiffSet.diffSetFromSdl(prev, sdl), r);
        List<String> breaking = r.getBreakages().stream().map(e -> e.getReasonMsg()).toList();
        if (n < 6) ok(breaking.isEmpty(), "SchemaDiff against the previous step: nothing BREAKING" + (breaking.isEmpty() ? "" : " " + breaking));
        else {
            var unexpected = breaking.stream().filter(m -> !m.contains("'Club.description'") && !m.contains("'Book.author'")).toList();
            ok(unexpected.isEmpty(), "the only breaking changes are the agreed removals" + (unexpected.isEmpty() ? "" : " " + unexpected));
            ok(field(s, "Club", "description") == null && field(s, "Book", "author") == null, "Club.description and Book.author are removed");
        }
        // Deprecation discipline: what a step replaces is deprecated from that step on
        if (n >= 1 && n < 6) ok(deprecated(s, "Club", "description"), "Club.description is deprecated (replaced in request 1)");
        if (n >= 3 && n < 6) ok(deprecated(s, "Book", "author"), "Book.author is deprecated (replaced in request 3)");
        if (n >= 4) ok(deprecated(s, "Review", "rating"), "Review.rating is deprecated (replaced in request 4)");
        // RFC #1231
        ok(validWithoutDeprecated(sdl), "valid with every deprecated element removed (RFC #1231)");
    }

    static GraphQLFieldDefinition field(GraphQLSchema s, String type, String name) {
        return s.getType(type) instanceof GraphQLFieldsContainer c ? c.getFieldDefinition(name) : null;
    }

    static boolean deprecated(GraphQLSchema s, String type, String name) {
        var f = field(s, type, name);
        return f != null && f.isDeprecated() && f.getDeprecationReason() != null && !f.getDeprecationReason().isBlank()
            && !f.getDeprecationReason().equals("No longer supported");
    }

    static boolean valid(GraphQLSchema s, String doc) {
        return !ParseAndValidate.parseAndValidate(s, ExecutionInput.newExecutionInput(doc).build()).isFailure();
    }

    static boolean validWithoutDeprecated(String sdl) {
        try {
            Document doc = Parser.parse(sdl);
            List<Definition> kept = doc.getDefinitions().stream().map(d -> switch (d) {
                case ObjectTypeDefinition o -> (Definition) o.transform(b -> b.fieldDefinitions(
                    o.getFieldDefinitions().stream().filter(f -> !f.hasDirective("deprecated")).toList()));
                case EnumTypeDefinition e -> e.transform(b -> b.enumValueDefinitions(
                    e.getEnumValueDefinitions().stream().filter(v -> !v.hasDirective("deprecated")).toList()));
                default -> d;
            }).toList();
            UnExecutableSchemaGenerator.makeUnExecutableSchema(new SchemaParser().parse(AstPrinter.printAst(doc.transform(b -> b.definitions(kept)))));
            return true;
        } catch (Exception e) { return false; }
    }

    static void revealRequest(int n) throws Exception {
        JsonNode cr = data.get("cr").get(n - 1);
        Files.createDirectories(Path.of("requests"));
        Files.createDirectories(Path.of("clients"));
        Files.writeString(Path.of(String.format("requests/%02d.md", n)), cr.get("text").asString());
        for (var c : cr.get("clients").properties()) Files.writeString(Path.of("clients", c.getKey()), c.getValue().asString());
        if (n == 6) {
            Files.createDirectories(Path.of("clients/v1"));
            for (var c : data.get("v1Clients").properties()) Files.writeString(Path.of("clients/v1", c.getKey()), c.getValue().asString());
        }
        System.out.println(n == 1 ? "Release 0 is live. The first change request has arrived:\n" : "The next change request has arrived:\n");
        System.out.println(cr.get("text").asString());
        System.out.printf("(saved as requests/%02d.md; its client is in clients/)%n", n);
        System.out.printf("Ship it as steps/%02d.graphqls (start from %s), then run Sim.java again.%n", n,
            n == 1 ? "v0.graphqls" : String.format("steps/%02d.graphqls", n - 1));
    }
}

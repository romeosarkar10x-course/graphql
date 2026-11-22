// Module 18 helpers for operations written for (and by) AI agents. Run through contract/agent/agent.sh:
//   agent ops <dir>                  check every <dir>/*.graphql as a tool's operation: parses under the September 2025
//                                    spec (Module 17's engine), one named operation with a description of 12 to 120
//                                    words that names each variable, valid against Shelfie once descriptions are
//                                    stripped (graphql-java 26.1 rejects them). Exit 1 if any file fails.
//   agent strip <file>               the document without descriptions, as graphql-java can parse it
//   agent cost <file>                Module 10's cost: a field costs 1 plus its children, times `first` (or `last`)
//                                    for a list, defaults included
//   agent score <questions.json> <dir>   score <dir>/qNN.graphql against the questions: valid, cost, executed against
//                                    Shelfie, and whether the response holds the answer (checked by hash, never shown)
import dev.shelfie.mini.Ast;
import dev.shelfie.mini.Parser;
import graphql.analysis.QueryComplexityCalculator;
import graphql.analysis.FieldComplexityCalculator;
import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.parser.InvalidSyntaxException;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.UnExecutableSchemaGenerator;
import graphql.validation.ValidationError;
import graphql.validation.Validator;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class Agent {
  static final String SHELFIE = System.getenv().getOrDefault("SHELFIE_URL", "http://shelfie-ref:8080");
  static final JsonMapper JSON = JsonMapper.builder().build();
  static final HttpClient HTTP = HttpClient.newHttpClient();

  public static void main(String[] a) throws Exception {
    if (a.length < 2) usage();
    switch (a[0]) {
      case "ops" -> System.exit(ops(Path.of(a[1])));
      case "strip" -> System.out.print(strip(Files.readString(Path.of(a[1]))));
      case "cost" -> System.out.println(cost(graphql.parser.Parser.parse(strip(Files.readString(Path.of(a[1])))), schema()));
      case "score" -> { if (a.length < 3) usage(); System.exit(score(Path.of(a[1]), Path.of(a[2]))); }
      default -> usage();
    }
  }

  static void usage() {
    System.err.println("usage: agent ops <dir> | strip <file> | cost <file> | score <questions.json> <dir>");
    System.exit(2);
  }

  // ---- descriptions -------------------------------------------------------------------------------------------------

  /** A string (block or plain) that starts an operation, a fragment or a variable definition: a description. */
  static final Pattern DESCRIPTION = Pattern.compile(
      "(\"\"\"(?:[^\"\\\\]|\\\\.|\"(?!\"\"))*\"\"\"|\"(?:[^\"\\\\\\n]|\\\\.)*\")\\s*(?=(?:query|mutation|subscription|fragment)\\b|\\$\\w+\\s*:)");

  /** Drop descriptions on executable definitions (September 2025) so a parser of the previous edition reads the rest. */
  static String strip(String doc) { return DESCRIPTION.matcher(doc).replaceAll(""); }

  static int words(String s) { return s.isBlank() ? 0 : s.trim().split("\\s+").length; }

  static int ops(Path dir) throws Exception {
    GraphQLSchema schema = schema();
    List<Path> files;
    try (var s = Files.list(dir)) { files = s.filter(p -> p.toString().endsWith(".graphql")).sorted().toList(); }
    if (files.isEmpty()) { System.out.println("fail " + dir + ": no .graphql files"); return 1; }
    int bad = 0;
    Set<String> names = new HashSet<>();
    for (Path f : files) {
      String text = Files.readString(f), file = f.getFileName().toString();
      List<String> problems = new ArrayList<>();
      Ast.Document spec = null;
      try { spec = Parser.parse(text); } catch (RuntimeException e) { problems.add("not valid under the September 2025 grammar: " + e.getMessage()); }
      String summary = "";
      if (spec != null) {
        var operations = spec.definitions().stream().filter(d -> d instanceof Ast.Operation).map(d -> (Ast.Operation) d).toList();
        if (operations.size() != 1) problems.add("one operation per file, found " + operations.size());
        for (Ast.Operation op : operations) {
          if (op.name() == null) problems.add("the operation needs a name (the tool's name comes from it)");
          else if (!names.add(op.name())) problems.add("operation name " + op.name() + " is used twice");
          String d = op.description();
          if (d == null) { problems.add("no description: put a \"\"\"string\"\"\" before `" + op.type() + "`"); continue; }
          int n = words(d);
          if (n < 12 || n > 120) problems.add("description has " + n + " words, want 12 to 120");
          for (Ast.VariableDef v : op.variables())
            if (!Pattern.compile("(?<![\\w$])\\$?" + Pattern.quote(v.name()) + "(?!\\w)").matcher(d).find())
              problems.add("the description never mentions $" + v.name());
          summary = op.type() + " " + op.name() + ", " + n + " words, " + op.variables().size() + " variable(s)";
        }
      }
      try {
        Document doc = graphql.parser.Parser.parse(strip(text));
        for (ValidationError e : new Validator().validateDocument(schema, doc, Locale.ENGLISH))
          problems.add("invalid against Shelfie: " + e.getDescription());
      } catch (InvalidSyntaxException e) { problems.add("graphql-java cannot parse it once descriptions are stripped: " + e.getMessage()); }
      if (problems.isEmpty()) System.out.println("ok   " + file + ": " + summary);
      else { bad++; System.out.println("fail " + file + ": " + String.join("; ", problems)); }
    }
    return bad == 0 ? 0 : 1;
  }

  // ---- schema, cost, execution ----------------------------------------------------------------------------------------

  static GraphQLSchema schema() throws Exception {
    var res = HTTP.send(HttpRequest.newBuilder(URI.create(SHELFIE + "/graphql/schema")).build(), HttpResponse.BodyHandlers.ofString());
    if (res.statusCode() != 200) throw new IllegalStateException("cannot read Shelfie's schema from " + SHELFIE + "/graphql/schema");
    return UnExecutableSchemaGenerator.makeUnExecutableSchema(new SchemaParser().parse(res.body()));
  }

  /** Module 10's function, with `last` counted like `first`. */
  static final FieldComplexityCalculator COST = (env, childCost) -> {
    Object n = env.getArguments().get("first");
    if (!(n instanceof Integer)) n = env.getArguments().get("last");
    return n instanceof Integer i ? Math.max(i, 1) * (1 + childCost) : 1 + childCost;
  };

  /** Cost before any variable is known: a required variable without a default gets a stand-in (1, true, "x"). */
  static int cost(Document doc, GraphQLSchema schema) {
    OperationDefinition op = doc.getDefinitionsOfType(OperationDefinition.class).getFirst();
    Map<String, Object> vars = new HashMap<>();
    for (var v : op.getVariableDefinitions()) {
      switch (v.getDefaultValue()) {   // a variable's own default wins over the argument's
        case graphql.language.IntValue i -> { vars.put(v.getName(), i.getValue().intValue()); continue; }
        case graphql.language.BooleanValue b -> { vars.put(v.getName(), b.isValue()); continue; }
        case graphql.language.StringValue t -> { vars.put(v.getName(), t.getValue()); continue; }
        case null, default -> {}
      }
      if (v.getDefaultValue() != null || !(v.getType() instanceof graphql.language.NonNullType nn)) continue;
      String type = nn.getType() instanceof graphql.language.TypeName t ? t.getName() : "";
      vars.put(v.getName(), switch (type) { case "Int" -> 1; case "Float" -> 1.0; case "Boolean" -> true; default -> "x"; });
    }
    return QueryComplexityCalculator.newCalculator().fieldComplexityCalculator(COST).schema(schema).document(doc)
        .operationName(op.getName()).variables(graphql.execution.CoercedVariables.of(vars)).build().calculate();
  }

  static JsonNode execute(String query) throws Exception {
    String body = JSON.writeValueAsString(Map.of("query", query));
    var req = HttpRequest.newBuilder(URI.create(SHELFIE + "/graphql")).header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
    return JSON.readTree(HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body());
  }

  // ---- scoring an agent's queries -------------------------------------------------------------------------------------

  /** The form answers are hashed in: trimmed, lower case, inner spaces collapsed; 5.0 is 5. */
  static String norm(JsonNode v) {
    String s = v.isNumber() && v.asDouble() == Math.rint(v.asDouble()) ? Long.toString((long) v.asDouble()) : v.asString();
    return s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
  }

  static String hash(String s) throws Exception {
    byte[] h = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(h).substring(0, 16);
  }

  static void leaves(JsonNode n, Set<String> out) throws Exception {
    if (n == null || n.isNull()) return;
    if (n.isContainer()) { for (JsonNode c : n) leaves(c, out); return; }
    out.add(hash(norm(n)));
  }

  static int score(Path questions, Path dir) throws Exception {
    GraphQLSchema schema = schema();
    JsonNode qs = JSON.readTree(Files.readString(questions)).get("questions");
    int valid = 0, answered = 0, total = 0, files = 0;
    long cost = 0;
    System.out.println("| # | Question | Valid | Cost | Answered | Problem |");
    System.out.println("|---|---|---|---|---|---|");
    for (JsonNode q : qs) {
      String id = q.get("id").asString();
      Path f = dir.resolve(id + ".graphql");
      String v = "no", c = "", ans = "no", problem = "";
      if (!Files.exists(f)) problem = "no " + f.getFileName();
      else {
        files++;
        try {
          Document doc = graphql.parser.Parser.parse(strip(Files.readString(f)));
          List<ValidationError> errors = new Validator().validateDocument(schema, doc, Locale.ENGLISH);
          if (!errors.isEmpty()) problem = errors.getFirst().getDescription();
          else {
            v = "yes"; valid++;
            boolean needsVariables = doc.getDefinitionsOfType(OperationDefinition.class).getFirst().getVariableDefinitions().stream()
                .anyMatch(d -> d.getDefaultValue() == null && d.getType() instanceof graphql.language.NonNullType);
            if (needsVariables) problem = "needs variables: write the values into the query; ";
            int n = cost(doc, schema); c = Integer.toString(n); cost += n;
            JsonNode res = execute(strip(Files.readString(f)));
            Set<String> got = new HashSet<>();
            leaves(res.get("data"), got);
            int need = 0, have = 0;
            for (JsonNode h : q.get("answers")) { need++; if (got.contains(h.asString())) have++; }
            if (have == need) { ans = "yes"; answered++; }
            else problem += have + " of " + need + " answer value(s) in the response" + (res.has("errors") ? "; it has errors" : "");
          }
        } catch (InvalidSyntaxException e) { problem = "syntax: " + e.getMessage().replace('|', '/'); }
      }
      total++;
      System.out.printf("| %s | %s | %s | %s | %s | %s |%n", id, q.get("question").asString(), v, c, ans, problem.replace("\n", " "));
    }
    System.out.printf("%nqueries %d of %d, valid %d, answered %d, total cost %d%n", files, total, valid, answered, cost);
    return files == total ? 0 : 1;
  }
}

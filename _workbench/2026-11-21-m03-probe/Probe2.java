import graphql.*;
import graphql.schema.idl.*;
import graphql.validation.rules.*;
import graphql.validation.schemawiring.ValidationSchemaWiring;
import java.util.*;
import java.util.concurrent.*;

public class Probe2 {
  enum Visibility { PUBLIC, PRIVATE }
  static GraphQL build(String sdl, RuntimeWiring w) {
    return GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl), w)).build();
  }
  static void run(String label, GraphQL g, String q, Map<String,Object> vars) {
    var r = g.execute(ExecutionInput.newExecutionInput(q).variables(vars == null ? Map.of() : vars).build());
    System.out.println("## " + label + "\n" + r.toSpecification());
  }
  public static void main(String[] a) throws Exception {
    // serial vs parallel
    long t0 = System.nanoTime();
    var log = new ConcurrentLinkedQueue<String>();
    graphql.schema.DataFetcher<Object> slow = e -> CompletableFuture.supplyAsync(() -> {
      String f = e.getField().getAlias() != null ? e.getField().getAlias() : e.getField().getName();
      log.add(String.format("start %-3s %4d ms %s", f, (System.nanoTime()-t0)/1_000_000, Thread.currentThread().getName().replaceAll("\\d+$","N")));
      try { Thread.sleep(200); } catch (InterruptedException x) {}
      log.add(String.format("end   %-3s %4d ms", f, (System.nanoTime()-t0)/1_000_000));
      return 1; }, Executors.newVirtualThreadPerTaskExecutor());
    var w = RuntimeWiring.newRuntimeWiring().type("Query", t -> t.dataFetcher("q", slow)).type("Mutation", t -> t.dataFetcher("m", slow)).build();
    var g = build("type Query { q: Int } type Mutation { m: Int }", w);
    long s = System.nanoTime();
    run("serial-vs-parallel query", g, "{ a: q b: q c: q }", null);
    System.out.println("query total ms " + (System.nanoTime()-s)/1_000_000); log.forEach(System.out::println); log.clear();
    s = System.nanoTime();
    run("serial-vs-parallel mutation", g, "mutation { a: m b: m c: m }", null);
    System.out.println("mutation total ms " + (System.nanoTime()-s)/1_000_000); log.forEach(System.out::println);
    // enums
    var g2 = build("type Query { v(in: Visibility): Visibility legacy: Visibility } enum Visibility { PUBLIC PRIVATE }",
      RuntimeWiring.newRuntimeWiring().type("Visibility", t -> t.enumValues(n -> switch (n) { case "PUBLIC" -> "P"; case "PRIVATE" -> "X"; default -> null; }))
       .type("Query", t -> t.dataFetcher("v", e -> { Object v = e.getArgument("in"); return v; }).dataFetcher("legacy", e -> "Q")).build());
    run("enum legacy codes in/out", g2, "{ v(in: PRIVATE) }", null);
    run("enum unknown internal value", g2, "{ legacy }", null);
    var g3 = build("type Query { v(in: Visibility): Visibility } enum Visibility { PUBLIC PRIVATE }",
      RuntimeWiring.newRuntimeWiring().type("Query", t -> t.dataFetcher("v", e -> { Object v = e.getArgument("in"); System.out.println("arg class " + v.getClass()); return Visibility.valueOf((String) v); })).build());
    run("java enum out by name", g3, "{ v(in: PUBLIC) }", null);
    // extended validation
    String sdl = "directive @Size(min : Int = 0, max : Int = 2147483647, message : String = \"graphql.validation.Size.message\") on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION\n"
      + "directive @Range(min : Int = 0, max : Int = 2147483647, message : String = \"graphql.validation.Range.message\") on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION\n"
      + "type Query { x: Int } type Mutation { createReview(input: ReviewInput!): String } input ReviewInput { rating: Int! @Range(min: 1, max: 5) body: String! @Size(min: 10, max: 2000) }";
    var rules = ValidationRules.newValidationRules().onValidationErrorStrategy(OnValidationErrorStrategy.RETURN_NULL).build();
    var vw = RuntimeWiring.newRuntimeWiring().directiveWiring(new ValidationSchemaWiring(rules))
      .type("Mutation", t -> t.dataFetcher("createReview", e -> "ok " + e.getArgument("input"))).build();
    var g4 = build(sdl, vw);
    run("ext-validation ok", g4, "mutation { createReview(input: {rating: 5, body: \"A fine long body\"}) }", null);
    run("ext-validation bad", g4, "mutation { createReview(input: {rating: 9, body: \"short\"}) }", null);
  }
}

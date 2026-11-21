// Probe: graphql-java 26.1 demand-control defaults and attack behaviour.
// bin/course java _workbench/2026-11-21-m10-probe/Limits.java
import graphql.*;
import graphql.analysis.*;
import graphql.execution.instrumentation.*;
import graphql.schema.*;
import graphql.schema.idl.*;
import graphql.validation.QueryComplexityLimits;
import java.util.*;

public class Limits {
  static final String SDL = """
      type Query { book: Book  books(first: Int = 10): [Book!]! hello: String }
      type Book { title: String  secretTitle: String  reviews(first: Int = 10): [Review!]! }
      type Review { body: String  book: Book }
      """;

  public static void main(String[] a) {
    var wiring = RuntimeWiring.newRuntimeWiring()
        .type("Query", t -> t.dataFetcher("book", e -> Map.of("title", "T"))
            .dataFetcher("books", e -> Collections.nCopies((int) e.getArgument("first"), Map.of("title", "T")))
            .dataFetcher("hello", e -> "hi"))
        .type("Book", t -> t.dataFetcher("reviews", e -> Collections.nCopies((int) e.getArgument("first"), Map.of("body", "B"))))
        .type("Review", t -> t.dataFetcher("book", e -> Map.of("title", "T")))
        .build();
    var schema = new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(SDL), wiring);
    var gql = GraphQL.newGraphQL(schema).build();

    System.out.println("defaults: " + QueryComplexityLimits.getDefaultLimits());
    run("1 depth 60", gql, deep(60), null);
    run("2 depth 120", gql, deep(120), null);
    run("3 typo (suggestions?)", gql, "{ book { titel } }", null);
    run("4 fragment cycle", gql, "{ book { ...A } } fragment A on Book { reviews { book { ...B } } } fragment B on Book { ...A }", null);
    run("5 10,000 aliases", gql, aliases(10_000), null);
    run("6 110,000 aliases", gql, aliases(110_000), null);
    var tight = QueryComplexityLimits.newLimits().maxDepth(5).maxFieldsCount(200).build();
    run("7 per-request limits depth 5, fragment-hidden depth 8", gql,
        "{ book { ...F } } fragment F on Book { reviews { book { reviews { book { reviews { body } } } } } }", tight);
    run("8 per-request limits 300 aliases", gql, aliases(300), tight);

    // MaxQueryDepthInstrumentation counts through fragments?
    var depthGql = GraphQL.newGraphQL(schema).instrumentation(new MaxQueryDepthInstrumentation(5)).build();
    run("9 MaxQueryDepthInstrumentation(5), fragment depth 8", depthGql,
        "{ book { ...F } } fragment F on Book { reviews { book { reviews { book { reviews { body } } } } } }", null);
    // Complexity: list fields cost first * child
    FieldComplexityCalculator calc = (env, child) -> {
      Object first = env.getArguments().get("first");
      return first instanceof Integer n ? n * (1 + child) : 1 + child;
    };
    var costGql = GraphQL.newGraphQL(schema).instrumentation(new MaxQueryComplexityInstrumentation(1000, calc)).build();
    run("10 complexity 1000: books(first:10){reviews(first:10){body}}", costGql, "{ books(first: 10) { reviews(first: 10) { body } } }", null);
    run("11 complexity 1000: books(first:50){reviews(first:50){body}}", costGql, "{ books(first: 50) { reviews(first: 50) { body } } }", null);
  }

  static String deep(int n) {
    var sb = new StringBuilder("{ book { ");
    for (int i = 0; i < n / 2; i++) sb.append("reviews(first: 1) { book { ");
    sb.append("title");
    for (int i = 0; i < n / 2; i++) sb.append(" } }");
    return sb.append(" } }").toString();
  }

  static String aliases(int n) {
    var sb = new StringBuilder("{ ");
    for (int i = 0; i < n; i++) sb.append("a").append(i).append(": hello ");
    return sb.append("}").toString();
  }

  static void run(String label, GraphQL gql, String q, QueryComplexityLimits limits) {
    var in = ExecutionInput.newExecutionInput(q);
    if (limits != null) in.graphQLContext(Map.of(QueryComplexityLimits.KEY, limits));
    long t = System.nanoTime();
    var r = gql.execute(in.build());
    long ms = (System.nanoTime() - t) / 1_000_000;
    String err = r.getErrors().isEmpty() ? "no errors" : r.getErrors().get(0).getMessage();
    if (err.length() > 160) err = err.substring(0, 160) + "...";
    System.out.printf("%-60s %5d ms  data=%s  %s%n", label, ms, r.getData() != null, err);
  }
}

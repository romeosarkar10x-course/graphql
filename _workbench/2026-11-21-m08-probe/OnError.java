import graphql.*;
import graphql.execution.*;
import graphql.schema.idl.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class OnError {
  static final String SDL = """
    type Query { club: Club! }
    type Club { name: String! owner: User! members: [User!]! currentBook: Book }
    type User { name: String! }
    type Book { title: String! rating: Float! }
    """;
  static class NullMode extends AsyncExecutionStrategy {
    @Override protected void assertNonNullFieldPrecondition(NonNullableFieldWasNullException e) {}
    @Override protected void assertNonNullFieldPrecondition(NonNullableFieldWasNullException e, CompletableFuture<?> f) {}
  }
  static GraphQL gql(ExecutionStrategy s, boolean halt) {
    var w = RuntimeWiring.newRuntimeWiring()
      .type("Query", t -> t.dataFetcher("club", e -> Map.of("name", "Midnight")))
      .type("Club", t -> t.dataFetcher("owner", e -> Map.of("name", "Kemi"))
        .dataFetcher("members", e -> Arrays.asList(Map.of("name", "a"), null, Map.of("name", "c")))
        .dataFetcher("currentBook", e -> Map.of("title", "Piranesi")))
      .type("Book", t -> t.dataFetcher("rating", e -> { throw new RuntimeException("ratings down"); }))
      .build();
    var b = GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(SDL), w));
    if (s != null) b.queryExecutionStrategy(s);
    if (halt) b.defaultDataFetcherExceptionHandler(p -> { throw new AbortExecutionException("halt: " + p.getException().getMessage()); });
    return b.build();
  }
  public static void main(String[] a) throws Exception {
    var q = "{ club { name owner { name } members { name } currentBook { title rating } } }";
    var m = new tools.jackson.databind.json.JsonMapper();
    System.out.println("PROPAGATE  " + m.writeValueAsString(gql(null, false).execute(q).toSpecification()));
    System.out.println("DIRECTIVE  " + m.writeValueAsString(gql(null, false).execute(q.replaceFirst("\\{", "query @experimental_disableErrorPropagation {")).toSpecification()));
    System.out.println("NULL-STRAT " + m.writeValueAsString(gql(new NullMode(), false).execute(q).toSpecification()));
    try { System.out.println("HALT       " + m.writeValueAsString(gql(null, true).execute(q).toSpecification())); }
    catch (Exception e) { System.out.println("HALT threw " + e); }
  }
}

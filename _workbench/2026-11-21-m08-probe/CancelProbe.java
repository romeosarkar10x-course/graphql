import graphql.*;
import graphql.schema.idl.*;
import java.util.*;
public class CancelProbe {
  public static void main(String[] a) {
    var log = new ArrayList<String>();
    var input = ExecutionInput.newExecutionInput("{ a b { x } c { x } }").build();
    var w = RuntimeWiring.newRuntimeWiring()
      .type("Query", t -> t.dataFetcher("a", e -> { log.add("a"); input.cancel(); log.add("cancelled=" + input.isCancelled()); return 1; })
        .dataFetcher("b", e -> { log.add("b"); return Map.of(); }).dataFetcher("c", e -> { log.add("c"); return Map.of(); }))
      .type("T", t -> t.dataFetcher("x", e -> { log.add("x"); return 1; })).build();
    var g = GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse("type Query { a: Int b: T c: T } type T { x: Int }"), w)).build();
    var r = g.execute(input);
    System.out.println(log + " -> " + r.toSpecification());
  }
}

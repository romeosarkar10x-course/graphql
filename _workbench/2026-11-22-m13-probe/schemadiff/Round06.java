import graphql.GraphQL;
import graphql.schema.idl.*;
import java.nio.file.*;
/** Does an old query (no sortBy) still validate and run against round 06's schema? */
public class Round06 {
  public static void main(String[] a) throws Exception {
    var reg = new SchemaParser().parse(Files.readString(Path.of(a[0])));
    var wiring = RuntimeWiring.newRuntimeWiring().type("Query", t -> t.dataFetcher("books", e -> {
      System.out.println("sortBy seen by the resolver: " + e.getArgument("sortBy")); return java.util.List.of(); })).build();
    var g = GraphQL.newGraphQL(new SchemaGenerator().makeExecutableSchema(reg, wiring)).build();
    System.out.println(g.execute("{ books(first: 2) { id } }").toSpecification());
  }
}

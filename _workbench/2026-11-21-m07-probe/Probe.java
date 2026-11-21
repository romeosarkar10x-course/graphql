import graphql.*;
import graphql.schema.idl.*;
import graphql.schema.diff.*;
import graphql.schema.diff.reporting.*;
import java.util.*;
public class Probe {
  public static void main(String[] a) {
    String sdl = "directive @experimental_disableErrorPropagation on QUERY | MUTATION | SUBSCRIPTION\n type Query { club: Club } type Club { name: String! book: Book! } type Book { title: String! rating: Float! }";
    var w = RuntimeWiring.newRuntimeWiring().type("Query", t -> t.dataFetcher("club", e -> Map.of("name","C", "book", Map.of("title","T"))))
      .type("Book", t -> t.dataFetcher("rating", e -> { throw new RuntimeException("down"); })).build();
    var s = new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl), w);
    var g = GraphQL.newGraphQL(s).build();
    System.out.println("enabled=" + Directives.isExperimentalDisableErrorPropagationDirectiveEnabled());
    System.out.println(g.execute("{ club { name book { title rating } } }").toSpecification());
    System.out.println(g.execute("query @experimental_disableErrorPropagation { club { name book { title rating } } }").toSpecification());
    String old = "type Query { a: String b(x: Int): Int e: E } enum E { X Y } input I { p: Int }";
    String[][] news = {
      {"remove field", "type Query { b(x: Int): Int e: E } enum E { X Y } input I { p: Int }"},
      {"add enum value", "type Query { a: String b(x: Int): Int e: E } enum E { X Y Z } input I { p: Int }"},
      {"add optional arg", "type Query { a: String b(x: Int, y: Int): Int e: E } enum E { X Y } input I { p: Int }"},
      {"add required arg", "type Query { a: String b(x: Int, y: Int!): Int e: E } enum E { X Y } input I { p: Int }"},
      {"nullable->nonnull output", "type Query { a: String! b(x: Int): Int e: E } enum E { X Y } input I { p: Int }"},
      {"arg nullable->nonnull", "type Query { a: String b(x: Int!): Int e: E } enum E { X Y } input I { p: Int }"},
      {"deprecate", "type Query { a: String @deprecated(reason: \"no\") b(x: Int): Int e: E } enum E { X Y } input I { p: Int }"},
      {"add field", "type Query { a: String b(x: Int): Int e: E c: Int } enum E { X Y } input I { p: Int }"},
    };
    for (var n : news) {
      var r = new CapturingReporter();
      new SchemaDiff().diffSchema(SchemaDiffSet.diffSetFromSdl(old, n[1]), r);
      System.out.println(n[0] + ": " + r.getEvents().stream().map(e -> e.getLevel() + "/" + e.getCategory() + " " + e.getReasonMsg()).toList());
    }
  }
}

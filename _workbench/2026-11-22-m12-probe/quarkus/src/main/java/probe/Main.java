package probe;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import io.smallrye.graphql.client.GraphQLClient;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.*;
import java.util.List;
import static io.smallrye.graphql.client.core.Document.document;
import static io.smallrye.graphql.client.core.Operation.operation;
import static io.smallrye.graphql.client.core.Field.field;
import static io.smallrye.graphql.client.core.Argument.arg;
import static io.smallrye.graphql.client.core.Argument.args;

@QuarkusMain
public class Main implements QuarkusApplication {
  public record Author(String name) {}
  public record Review(int rating, Author author) {}
  public static class Book { public String title; public List<String> authors; public List<Review> reviews; public String toString() { return title + " " + authors + " " + reviews; } }

  @GraphQLClientApi(configKey = "shelfie")
  public interface Shelfie {
    @Query Book book(@NonNull @Id String id);
    @Query("books") List<Book> books(String titleContains, Integer first);
  }
  @Inject Shelfie typed;
  @Inject @GraphQLClient("shelfie") DynamicGraphQLClient dynamic;

  public int run(String... a) throws Exception {
    System.out.println("typed: " + typed.book("b1"));
    System.out.println("typed list: " + typed.books("the", 2).size());
    var doc = document(operation(field("book", args(arg("id", "b1")), field("title"), field("authors"))));
    System.out.println("dynamic doc: " + doc.build());
    var r = dynamic.executeSync(doc);
    System.out.println("dynamic: " + r.getObject(Book.class, "book") + " errors=" + r.getErrors());
    var bad = dynamic.executeSync("{ book(id: \"b1\") { nope } }");
    System.out.println("dynamic bad: hasErrors=" + r.hasError() + " " + bad.getErrors());
    try { typed.book("nope"); System.out.println("typed nope: " + typed.book("nope")); } catch (Exception e) { System.out.println("typed nope ex " + e); }
    return 0;
  }
}

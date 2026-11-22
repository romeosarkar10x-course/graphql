package dev.shelfie.mp;

import dev.shelfie.mp.Model.*;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.smallrye.graphql.client.GraphQLClient;
import io.smallrye.graphql.client.Response;
import io.smallrye.graphql.client.dynamic.api.DynamicGraphQLClient;
import jakarta.inject.Inject;
import java.util.Map;

import static io.smallrye.graphql.client.core.Argument.arg;
import static io.smallrye.graphql.client.core.Argument.args;
import static io.smallrye.graphql.client.core.Document.document;
import static io.smallrye.graphql.client.core.Field.field;
import static io.smallrye.graphql.client.core.Operation.operation;
import static io.smallrye.graphql.client.core.OperationType.QUERY;
import static io.smallrye.graphql.client.core.ScalarType.GQL_ID;
import static io.smallrye.graphql.client.core.Variable.var;
import static io.smallrye.graphql.client.core.Variable.vars;
import static io.smallrye.graphql.client.core.VariableType.nonNull;

/** Lab 12.4: the same club card two ways. Usage: java -jar target/quarkus-app/quarkus-run.jar typesafe|dynamic <clubId>. */
@QuarkusMain
public class Main implements QuarkusApplication {
  @Inject ShelfieApi typed;
  @Inject @GraphQLClient("shelfie") DynamicGraphQLClient dynamic;

  @Override
  public int run(String... args) throws Exception {
    String mode = args.length > 0 ? args[0] : "typesafe";
    String id = args.length > 1 ? args[1] : "c1";
    Club club = switch (mode) {
      case "typesafe" -> typed.club(id, 2);
      case "dynamic" -> dynamicClub(id);
      default -> throw new IllegalArgumentException("mode must be typesafe or dynamic");
    };
    print(id, club);
    return 0;
  }

  /** The dynamic client: the document built in Java (or a string), the result read by path. */
  private Club dynamicClub(String id) throws Exception {
    var idVar = var("id", nonNull(GQL_ID));
    var doc = document(operation(QUERY, "clubCard", vars(idVar),
        field("club", args(arg("id", idVar)),
            field("name"), field("memberCount"),
            field("owner", field("name")),
            field("currentBook", field("title"), field("authors"), field("publishedYear"),
                field("reviews", args(arg("last", 2)), field("rating"), field("body"), field("author", field("name")))))));
    Response response = dynamic.executeSync(doc, Map.of("id", id));
    if (response.hasError()) throw new IllegalStateException(response.getErrors().toString());
    return response.getObject(Club.class, "club");
  }

  private static void print(String id, Club club) {
    if (club == null) {
      System.out.println("No club " + id);
      return;
    }
    System.out.printf("%s (%d members), run by %s%n", club.name(), club.memberCount(), club.owner().name());
    Book b = club.currentBook();
    if (b == null) {
      System.out.println("Reading: nothing right now");
      return;
    }
    System.out.printf("Reading: %s by %s (%d)%n", b.title(), String.join(", ", b.authors()), b.publishedYear());
    for (Review r : b.reviews()) System.out.printf("  %d/5 %s: %s%n", r.rating(), r.author().name(), r.body());
  }
}

package dev.shelfie.mp;

import dev.shelfie.mp.Model.*;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * Lab 12.4: the same club card two ways. Usage: java -jar target/quarkus-app/quarkus-run.jar typesafe|dynamic <clubId>.
 * Add a @GraphQLClientApi interface (config key "shelfie", see application.properties) and inject it; inject a
 * DynamicGraphQLClient with @GraphQLClient("shelfie"). Both must return a Model.Club.
 */
@QuarkusMain
public class Main implements QuarkusApplication {

  @Override
  public int run(String... args) throws Exception {
    String mode = args.length > 0 ? args[0] : "typesafe";
    String id = args.length > 1 ? args[1] : "c1";
    Club club = switch (mode) {
      case "typesafe" -> todo("the typesafe client");
      case "dynamic" -> todo("the dynamic client");
      default -> throw new IllegalArgumentException("mode must be typesafe or dynamic");
    };
    print(id, club);
    return 0;
  }

  private static Club todo(String what) {
    throw new UnsupportedOperationException("TODO: " + what);
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

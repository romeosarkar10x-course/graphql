package dev.shelfie.typed;

import dev.shelfie.typed.generated.client.BooksGraphQLQuery;
import dev.shelfie.typed.generated.client.BooksProjectionRoot;
import dev.shelfie.typed.generated.client.ClubGraphQLQuery;
import dev.shelfie.typed.generated.client.ClubProjectionRoot;
import dev.shelfie.typed.generated.types.Book;
import dev.shelfie.typed.generated.types.Club;
import dev.shelfie.typed.generated.types.Review;
import java.util.List;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.DgsGraphQlClient;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.web.client.RestClient;

/**
 * Lab 12.3: the club page through generated, typed requests. No document text anywhere: each root field is
 * a generated GraphQLQuery, each selection a generated projection, each result a generated type. A field
 * renamed in the schema breaks this file at compile time.
 */
public class ClubPage {
  public static void main(String[] args) {
    String url = args.length > 0 ? args[0] : "http://shelfie-ref:8080/graphql";
    String clubId = args.length > 1 ? args[1] : "c1";
    DgsGraphQlClient client = DgsGraphQlClient.create(HttpSyncGraphQlClient.create(RestClient.create(url)));

    // Two root fields, one HTTP request (DgsGraphQlClient's multi-query support, Spring for GraphQL 2.0).
    ClientGraphQlResponse response = client
        .request(ClubGraphQLQuery.newRequest().id(clubId).build())
        .projection(new ClubProjectionRoot<>()
            .name().memberCount()
            .owner().name().parent()
            .currentBook().title().authors().publishedYear()
                .reviews(2).rating().body().author().name().root())
        .request(BooksGraphQLQuery.newRequest().first(3).build())
        .projection(new BooksProjectionRoot<>().title().averageRating())
        .executeSync();

    Club club = response.field("club").toEntity(Club.class);
    if (club == null) {
      System.out.println("No club " + clubId);
      return;
    }
    System.out.printf("%s (%d members), run by %s%n", club.getName(), club.getMemberCount(), club.getOwner().getName());
    Book current = club.getCurrentBook();
    if (current == null) {
      System.out.println("Reading: nothing right now");
    } else {
      System.out.printf("Reading: %s by %s (%d)%n", current.getTitle(), String.join(", ", current.getAuthors()), current.getPublishedYear());
      for (Review r : current.getReviews()) {
        System.out.printf("  %d/5 %s: %s%n", r.getRating(), r.getAuthor().getName(), r.getBody());
      }
    }
    List<Book> popular = response.field("books").toEntityList(Book.class);
    System.out.println("In the catalogue:");
    for (Book b : popular) {
      System.out.printf("  %s%s%n", b.getTitle(), b.getAverageRating() == null ? "" : " (" + b.getAverageRating() + ")");
    }
  }
}

package probe;

import org.springframework.graphql.client.*;
import org.springframework.web.client.RestClient;
import probe.gen.client.*;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;
import probe.gen.types.*;
import java.util.List;

public class Main {
  public static void main(String[] a) {
    var http = HttpSyncGraphQlClient.create(RestClient.create(a.length > 0 ? a[0] : "http://shelfie-ref:8080/graphql"));
    var dgs = DgsGraphQlClient.create(http);
    Book b = dgs.request(BookGraphQLQuery.newRequest().id("b1").build())
        .projection(new BookProjectionRoot<>().title().authors().reviews(2).rating().author().name())
        .retrieveSync().toEntity(Book.class);
    System.out.println(b.getTitle() + " " + b.getAuthors() + " " + b.getReviews().size() + " " + b.getReviews().get(0).getAuthor().getName());
    // multi-query
    var resp = dgs.request(ClubGraphQLQuery.newRequest().id("c1").build()).projection(new ClubProjectionRoot<>().name().memberCount())
        .request(BooksGraphQLQuery.newRequest().first(2).build()).projection(new BooksProjectionRoot<>().title())
        .executeSync();
    System.out.println(resp.field("club").toEntity(Club.class).getName() + " " + resp.field("books").toEntityList(Book.class).size());
    System.out.println(new GraphQLQueryRequest(BookGraphQLQuery.newRequest().id("b1").build(), new BookProjectionRoot<>().title()).serialize());
  }
}

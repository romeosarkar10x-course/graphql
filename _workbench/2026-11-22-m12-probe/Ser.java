import dev.shelfie.typed.generated.client.*;
import com.netflix.graphql.dgs.client.codegen.GraphQLQueryRequest;
public class Ser { public static void main(String[] a) {
  System.out.println(new GraphQLQueryRequest(ClubGraphQLQuery.newRequest().id("c1").build(), new ClubProjectionRoot<>().name().currentBook().title().reviews(2).rating().root()).serialize());
}}

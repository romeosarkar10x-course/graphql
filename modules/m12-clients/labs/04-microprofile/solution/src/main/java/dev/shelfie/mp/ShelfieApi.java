package dev.shelfie.mp;

import dev.shelfie.mp.Model.Club;
import io.smallrye.graphql.client.typesafe.api.GraphQLClientApi;
import io.smallrye.graphql.client.typesafe.api.NestedParameter;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;

/**
 * The typesafe client: an interface. The method is the root field, its parameters the arguments (sent as
 * variables), the return type the selection set. @NestedParameter puts an argument on a nested field
 * (Book.reviews(last:)). @NonNull matters: a plain String parameter is declared as `ID`, and Shelfie's
 * `club(id: ID!)` refuses it (VariableTypeMismatch); a primitive int is already `Int!`.
 *
 * Sent: query club($id: ID!, $last: Int!) { club(id: $id) {name memberCount owner {name} currentBook {...
 * reviews(last: $last) {...}}} } with operationName "club".
 */
@GraphQLClientApi(configKey = "shelfie")
public interface ShelfieApi {
  @Query
  Club club(@NonNull @Id String id, @NestedParameter("currentBook.reviews") @Name("last") int last);
}

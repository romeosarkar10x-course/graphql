package dev.shelfie.quarkus;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Id;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;

/** Code-first: the schema is generated from these method signatures (see /graphql/schema.graphql). */
@GraphQLApi
public class ShelfieApi {
    static final AtomicInteger BOOK_BATCHES = new AtomicInteger();

    @Query
    @Description("One club by id, or null")
    public Types.Club club(@Name("id") String id) {
        return Data.club(id).map(Types.Club::from).orElse(null);
    }

    @Query
    @NonNull
    public List<Types.@NonNull Club> clubs() { return Data.CLUBS.stream().map(Types.Club::from).toList(); }

    // TODO: Club.owner, Club.currentBook (as ONE batch call per response, counted in BOOK_BATCHES) and
    // Book.averageRating, with @Source parameters. And make the generated schema say ID! and String!
    // where Shelfie's does (look at /graphql/schema.graphql).

    @Query
    @Description("How many times the currentBook batch has run (for the lab test)")
    public int currentBookBatches() { return BOOK_BATCHES.get(); }
}

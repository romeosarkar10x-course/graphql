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
    public Types.Club club(@Name("id") @Id @NonNull String id) {
        return Data.club(id).map(Types.Club::from).orElse(null);
    }

    @Query
    @NonNull
    public List<Types.@NonNull Club> clubs() { return Data.CLUBS.stream().map(Types.Club::from).toList(); }

    /** A @Source parameter adds the field to Club. */
    @NonNull
    public Types.User owner(@Source Types.Club club) { return Types.User.from(Data.user(club.ownerId).orElseThrow()); }

    /** A List<@Source> parameter makes it a batch: one call for every club in the response. */
    public List<Types.Book> currentBook(@Source List<Types.Club> clubs) {
        BOOK_BATCHES.incrementAndGet();
        return clubs.stream().map(c -> Data.book(c.currentBookId).map(Types.Book::from).orElse(null)).toList();
    }

    public Double averageRating(@Source Types.Book book) { return Data.averageRating(book.id); }

    @Query
    @Description("How many times the currentBook batch has run (for the lab test)")
    public int currentBookBatches() { return BOOK_BATCHES.get(); }
}

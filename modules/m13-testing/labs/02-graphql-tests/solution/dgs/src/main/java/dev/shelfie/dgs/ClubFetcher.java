package dev.shelfie.dgs;

import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
import dev.shelfie.dgs.generated.DgsConstants;
import dev.shelfie.dgs.generated.types.Book;
import dev.shelfie.dgs.generated.types.Club;
import dev.shelfie.dgs.generated.types.User;
import java.util.List;

/**
 * DGS's programming model on top of Spring for GraphQL. The types and DgsConstants are generated from
 * src/main/resources/schema at build time: rename a schema field and this class stops compiling.
 */
@DgsComponent
public class ClubFetcher {

    static Club toGraphQL(Data.Club c) {
        var owner = Data.user(c.ownerId()).orElseThrow();
        return Club.newBuilder().id(c.id()).name(c.name()).memberCount(c.memberIds().size())
            .owner(User.newBuilder().id(owner.id()).name(owner.name()).handle(owner.handle()).build())
            .build();
    }

    static Book toGraphQL(Data.Book b) { return Book.newBuilder().id(b.id()).title(b.title()).build(); }

    @DgsQuery
    public Club club(@InputArgument String id) { return Data.club(id).map(ClubFetcher::toGraphQL).orElse(null); }

    @DgsQuery
    public List<Club> clubs() { return Data.CLUBS.stream().map(ClubFetcher::toGraphQL).toList(); }

    @DgsQuery
    public Book book(@InputArgument String id) { return Data.book(id).map(ClubFetcher::toGraphQL).orElse(null); }

    /** Resolved only when asked for: the generated Club has the field, but we leave it unset above. */
    @DgsData(parentType = DgsConstants.CLUB.TYPE_NAME, field = DgsConstants.CLUB.CurrentBook)
    public Book currentBook(DgsDataFetchingEnvironment env) {
        Club club = env.getSource();
        return Data.club(club.getId()).flatMap(c -> Data.book(c.currentBookId())).map(ClubFetcher::toGraphQL).orElse(null);
    }

    @DgsData(parentType = DgsConstants.BOOK.TYPE_NAME, field = DgsConstants.BOOK.AverageRating)
    public Double averageRating(DgsDataFetchingEnvironment env) {
        Book book = env.getSource();
        return Data.averageRating(book.getId());
    }
}

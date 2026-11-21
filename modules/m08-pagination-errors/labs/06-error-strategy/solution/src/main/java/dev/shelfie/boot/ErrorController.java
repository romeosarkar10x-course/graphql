package dev.shelfie.boot;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/** The fields in errors.graphqls. They throw ShelfieException for the contract's failures. */
@Controller
public class ErrorController {
    private final Set<String> joined = ConcurrentHashMap.newKeySet();   // "clubId:userId" joined since start

    static String signedIn(String viewer) {
        if (viewer == null) throw new ShelfieException(ShelfieError.UNAUTHENTICATED, "Sign in first (X-Shelfie-User header)");
        return viewer;
    }

    @QueryMapping
    public List<Data.Club> myClubs(@ContextValue(name = "viewer", required = false) String viewer) {
        return Data.clubsOf(signedIn(viewer));
    }

    @MutationMapping
    public Data.Club joinClub(@Argument String clubId, @ContextValue(name = "viewer", required = false) String viewer) {
        String user = signedIn(viewer);
        var club = Data.club(clubId).filter(c -> c.visibility().equals("PUBLIC") || c.memberIds().contains(user))
            .orElseThrow(() -> new ShelfieException(ShelfieError.NOT_FOUND, "No club " + clubId));
        if (club.memberIds().contains(user) || !joined.add(clubId + ":" + user))
            throw new ShelfieException(ShelfieError.CONFLICT, "Already a member of " + clubId);
        return club;
    }

    @MutationMapping
    public boolean deleteReview(@Argument String id, @ContextValue(name = "viewer", required = false) String viewer) {
        String user = signedIn(viewer);
        synchronized (Data.REVIEWS) {
            var review = Data.REVIEWS.stream().filter(r -> r.id().equals(id)).findFirst()
                .orElseThrow(() -> new ShelfieException(ShelfieError.NOT_FOUND, "No review " + id));
            if (!review.authorId().equals(user)) throw new ShelfieException(ShelfieError.FORBIDDEN, "Not your review");
            return Data.REVIEWS.remove(review);
        }
    }

    /** An outage, written the way real ones look: the message is full of internals. */
    @SchemaMapping(typeName = "Book")
    public String coverUrl(Data.Book book) {
        throw new IllegalStateException("S3 GetObject s3://shelfie-covers-prod/" + book.isbn() + ".jpg failed: connect to 10.0.4.17:9000 refused");
    }
}

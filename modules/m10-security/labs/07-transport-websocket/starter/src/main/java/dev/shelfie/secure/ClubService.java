package dev.shelfie.secure;

import org.springframework.graphql.execution.ErrorType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * The writes, guarded where the business logic lives (lecture 10.2), not in the controller: a REST
 * endpoint or a batch job calling this service gets the same rules. A refused call throws
 * AccessDeniedException; Spring for GraphQL turns it into FORBIDDEN (signed in) or UNAUTHORIZED (anonymous).
 */
@Service
public class ClubService {
  private final Store store;

  public ClubService(Store store) { this.store = store; }

  @PreAuthorize("@access.isMember(#clubId)")
  public Review addReview(String clubId, String bookId, int rating, String body, Reader author) {
    if (!store.books.containsKey(bookId)) throw new Denied(ErrorType.BAD_REQUEST, "no book " + bookId);
    return store.addReview(clubId, bookId, rating, body, author.id());
  }

  @PreAuthorize("@access.canRemoveReview(#id)")
  public String removeReview(String id) {
    store.reviews.remove(id);
    return id;
  }

  @PreAuthorize("@access.ownsClub(#id)")
  public Club rename(String id, String name) { return store.rename(id, name); }
}

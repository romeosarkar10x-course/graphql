import java.util.List;
import java.util.Optional;

/**
 * The redesigned inputs as Java records. A patch input uses three states per field:
 * null = absent (do not touch), Optional.empty() = sent as null (clear it), Optional.of(v) = set it.
 */
final class Inputs {
    private Inputs() {}

    enum Visibility { PUBLIC, PRIVATE }
    enum ReadingStatus { WANT_TO_READ, READING, FINISHED }

    record CreateReviewInput(String bookId, int rating, String body) {}
    record UpdateClubInput(String clubId, Optional<String> name, Optional<String> description, Optional<Visibility> visibility) {}
    record JoinClubInput(String clubId) {}
    record LeaveClubInput(String clubId) {}
    record SetShelfStatusInput(String bookId, ReadingStatus status, boolean notifyFollowers) {}
    record DeleteReviewsInput(List<String> reviewIds) {}
}

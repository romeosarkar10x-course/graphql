import java.util.List;
import java.util.Optional;

/**
 * The redesigned inputs as Java records. Names and components must match your redesign.graphqls and
 * the README (the test binds into them by name).
 */
final class Inputs {
    private Inputs() {}

    record CreateReviewInput(String bookId, int rating, String body) {}
    // TODO: Visibility and ReadingStatus enums; UpdateClubInput (a patch: README explains the three
    // states), JoinClubInput, LeaveClubInput, SetShelfStatusInput, DeleteReviewsInput.
}

package dev.shelfie.ref.graphql;

import dev.shelfie.ref.data.Model.Book;
import dev.shelfie.ref.data.Model.Club;
import dev.shelfie.ref.data.Model.Review;
import dev.shelfie.ref.data.Store;
import dev.shelfie.ref.storage.CoverStorage;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

/**
 * Module 4 writes. Rules live here, once, and both error styles (payload with {@code userErrors},
 * result union) are rendered from the same {@link Problem} list, so lab 4.3 compares styles, not rules.
 */
@Controller
public class MutationController {

    // ---- GraphQL types as records; union members resolve by simple class name ----
    public record CreateReviewInput(String bookId, int rating, String body) {}
    public record IdInput(String id) {}
    public record ClubInput(String clubId) {}
    public record CreateCoverUploadInput(String bookId, String contentType) {}
    public record ConfirmCoverUploadInput(String uploadId) {}

    public record UserError(List<String> field, String message, String code) {}
    public record CreateReviewPayload(Review review, List<UserError> userErrors) {}
    public record DeleteReviewPayload(String deletedReviewId, Book book, List<UserError> userErrors) {}
    public record JoinClubPayload(Club club, List<UserError> userErrors) {}
    public record LeaveClubPayload(Club club, List<UserError> userErrors) {}
    public record UploadCoverPayload(Book book, Long bytes, String sha256, Long serverBufferedBytes, List<UserError> userErrors) {}
    public record CreateCoverUploadPayload(String uploadUrl, String uploadId, List<UserError> userErrors) {}

    public sealed interface CreateReviewResult permits ReviewCreated, ValidationFailed, NotAllowed {}
    public record ReviewCreated(Review review) implements CreateReviewResult {}
    public record FieldProblem(List<String> field, String message) {}
    public record ValidationFailed(List<FieldProblem> problems) implements CreateReviewResult {}
    public record NotAllowed(String reason) implements CreateReviewResult {}

    record Problem(String code, List<String> field, String message) {
        UserError toUserError() { return new UserError(field, message, code); }
    }

    private static final long MAX_COVER = 100L * 1024 * 1024;
    private final Store store;
    private final CoverStorage storage;

    public MutationController(Store store, CoverStorage storage) {
        this.store = store;
        this.storage = storage;
    }

    // ---- reviews ----

    private List<Problem> reviewProblems(String viewer, CreateReviewInput in) {
        List<Problem> p = new ArrayList<>();
        if (viewer == null || store.user(viewer).isEmpty()) {
            p.add(new Problem("UNAUTHENTICATED", null, "Sign in first: send an X-Shelfie-User header naming a user, e.g. u1."));
            return p;
        }
        if (store.book(in.bookId()).isEmpty()) {
            p.add(new Problem("NOT_FOUND", List.of("input", "bookId"), "No book with id " + in.bookId() + "."));
        } else if (store.reviewOf(in.bookId(), viewer).isPresent()) {
            p.add(new Problem("DUPLICATE", List.of("input", "bookId"), "You already reviewed this book."));
        }
        if (in.rating() < 1 || in.rating() > 5) {
            p.add(new Problem("INVALID", List.of("input", "rating"), "Rating must be from 1 to 5."));
        }
        int len = in.body().strip().length();
        if (len < 10 || len > 2000) {
            p.add(new Problem("INVALID", List.of("input", "body"), "Body must be 10 to 2000 characters (got " + len + ")."));
        }
        return p;
    }

    private Review insert(String viewer, CreateReviewInput in) {
        return store.addReview(in.bookId(), viewer, in.rating(), in.body().strip(),
                Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
    }

    @MutationMapping
    public CreateReviewPayload createReview(@Argument CreateReviewInput input, @ContextValue(required = false) String viewer) {
        synchronized (store) {
            List<Problem> p = reviewProblems(viewer, input);
            if (!p.isEmpty()) return new CreateReviewPayload(null, p.stream().map(Problem::toUserError).toList());
            return new CreateReviewPayload(insert(viewer, input), List.of());
        }
    }

    @MutationMapping
    public CreateReviewResult createReviewTyped(@Argument CreateReviewInput input, @ContextValue(required = false) String viewer) {
        synchronized (store) {
            List<Problem> p = reviewProblems(viewer, input);
            if (p.isEmpty()) return new ReviewCreated(insert(viewer, input));
            if (p.getFirst().code().equals("UNAUTHENTICATED")) return new NotAllowed(p.getFirst().message());
            return new ValidationFailed(p.stream().map(x -> new FieldProblem(x.field(), x.message())).toList());
        }
    }

    @MutationMapping
    public DeleteReviewPayload deleteReview(@Argument IdInput input, @ContextValue(required = false) String viewer) {
        synchronized (store) {
            if (viewer == null || store.user(viewer).isEmpty()) return new DeleteReviewPayload(null, null, List.of(unauthenticated()));
            var review = store.review(input.id());
            if (review.isEmpty()) return new DeleteReviewPayload(null, null,
                    List.of(new UserError(List.of("input", "id"), "No review with id " + input.id() + ".", "NOT_FOUND")));
            if (!review.get().authorId().equals(viewer)) return new DeleteReviewPayload(null, null,
                    List.of(new UserError(List.of("input", "id"), "Only the author can delete a review.", "FORBIDDEN")));
            store.removeReview(input.id());
            return new DeleteReviewPayload(input.id(), store.book(review.get().bookId()).orElseThrow(), List.of());
        }
    }

    // ---- clubs ----

    @MutationMapping
    public JoinClubPayload joinClub(@Argument ClubInput input, @ContextValue(required = false) String viewer) {
        synchronized (store) {
            if (viewer == null || store.user(viewer).isEmpty()) return new JoinClubPayload(null, List.of(unauthenticated()));
            var club = store.club(input.clubId());
            if (club.isEmpty()) return new JoinClubPayload(null, List.of(clubNotFound(input.clubId())));
            Club c = club.get();
            if (c.memberIds().contains(viewer)) return new JoinClubPayload(c, List.of());
            if (c.visibility().equals("PRIVATE")) return new JoinClubPayload(null, List.of(
                    new UserError(List.of("input", "clubId"), "This club is private: ask the owner for an invite.", "FORBIDDEN")));
            if (c.memberIds().size() >= c.rules().maxMembers()) return new JoinClubPayload(null, List.of(
                    new UserError(List.of("input", "clubId"), "This club is full.", "INVALID")));
            List<String> members = new ArrayList<>(c.memberIds());
            members.add(viewer);
            return new JoinClubPayload(store.setMembers(c.id(), members), List.of());
        }
    }

    @MutationMapping
    public LeaveClubPayload leaveClub(@Argument ClubInput input, @ContextValue(required = false) String viewer) {
        synchronized (store) {
            if (viewer == null || store.user(viewer).isEmpty()) return new LeaveClubPayload(null, List.of(unauthenticated()));
            var club = store.club(input.clubId());
            if (club.isEmpty()) return new LeaveClubPayload(null, List.of(clubNotFound(input.clubId())));
            Club c = club.get();
            if (c.ownerId().equals(viewer)) return new LeaveClubPayload(null, List.of(
                    new UserError(List.of("input", "clubId"), "The owner cannot leave their own club.", "FORBIDDEN")));
            if (!c.memberIds().contains(viewer)) return new LeaveClubPayload(c, List.of());
            List<String> members = new ArrayList<>(c.memberIds());
            members.remove(viewer);
            return new LeaveClubPayload(store.setMembers(c.id(), members), List.of());
        }
    }

    // ---- covers (4.6) ----

    @MutationMapping
    public UploadCoverPayload uploadCover(@Argument String bookId, @Argument MultipartFile file) throws Exception {
        var book = store.book(bookId);
        if (book.isEmpty()) return coverError(List.of("bookId"), "No book with id " + bookId + ".", "NOT_FOUND");
        if (file.getSize() > MAX_COVER) return coverError(List.of("file"), "Covers are at most 100 MB.", "INVALID");
        byte[] bytes = file.getBytes();   // the whole file, on the heap: the point of the 4.6 experiment
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        store.setCover(bookId, "https://cdn.shelfie.dev/uploads/" + bookId + "/" + sha.substring(0, 12));
        return new UploadCoverPayload(book.get(), (long) bytes.length, sha, (long) bytes.length, List.of());
    }

    @MutationMapping
    public CreateCoverUploadPayload createCoverUpload(@Argument CreateCoverUploadInput input) {
        if (store.book(input.bookId()).isEmpty()) return new CreateCoverUploadPayload(null, null,
                List.of(new UserError(List.of("input", "bookId"), "No book with id " + input.bookId() + ".", "NOT_FOUND")));
        if (!List.of("image/jpeg", "image/png").contains(input.contentType())) return new CreateCoverUploadPayload(null, null,
                List.of(new UserError(List.of("input", "contentType"), "Use image/jpeg or image/png.", "INVALID")));
        Map.Entry<String, String> up = storage.createUpload(input.bookId(), input.contentType());
        return new CreateCoverUploadPayload(up.getValue(), up.getKey(), List.of());
    }

    @MutationMapping
    public UploadCoverPayload confirmCoverUpload(@Argument ConfirmCoverUploadInput input) throws Exception {
        var pending = storage.pending(input.uploadId());
        if (pending.isEmpty()) return coverError(List.of("input", "uploadId"), "Unknown or already confirmed upload.", "NOT_FOUND");
        var seen = storage.inspect(pending.get());
        if (seen.isEmpty()) return coverError(List.of("input", "uploadId"), "Nothing was uploaded to the URL yet.", "INVALID");
        storage.forget(input.uploadId());
        store.setCover(pending.get().bookId(), storage.objectUrl(pending.get()));
        return new UploadCoverPayload(store.book(pending.get().bookId()).orElseThrow(), seen.get().bytes(), seen.get().sha256(),
                (long) CoverStorage.BUFFER, List.of());
    }

    @SchemaMapping(typeName = "Book", field = "coverUrl")
    public String coverUrl(Book book) {
        return store.coverUrl(book);
    }

    private static UploadCoverPayload coverError(List<String> field, String message, String code) {
        return new UploadCoverPayload(null, null, null, null, List.of(new UserError(field, message, code)));
    }

    private static UserError unauthenticated() {
        return new UserError(null, "Sign in first: send an X-Shelfie-User header naming a user, e.g. u1.", "UNAUTHENTICATED");
    }

    private static UserError clubNotFound(String id) {
        return new UserError(List.of("input", "clubId"), "No club with id " + id + ".", "NOT_FOUND");
    }
}

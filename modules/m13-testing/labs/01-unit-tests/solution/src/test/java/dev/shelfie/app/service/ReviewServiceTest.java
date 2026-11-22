package dev.shelfie.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.shelfie.app.store.CatalogRepository;
import dev.shelfie.app.store.Records.Book;
import dev.shelfie.app.store.Records.Review;
import dev.shelfie.app.store.ReviewRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.test.StepVerifier;

/** The review rules with no Spring and no GraphQL: two mocked repositories and a fixed clock. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceTest {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-21T12:00:00Z"), ZoneOffset.UTC);
    static final Book B1 = new Book("b1", "The Left Hand of Darkness", List.of("Ursula K. Le Guin"), "9780441478125", 1969);

    @Mock ReviewRepository repo;
    @Mock CatalogRepository catalog;
    ReviewService service;

    @BeforeEach
    void setUp() {
        service = new ReviewService(repo, catalog, CLOCK);
        when(catalog.book("b1")).thenReturn(Optional.of(B1));
        when(catalog.book("nope")).thenReturn(Optional.empty());
        when(repo.all()).thenReturn(List.of());
        when(repo.add(anyString(), anyString(), anyInt(), anyString(), anyString()))
            .thenAnswer(i -> new Review("r99", i.getArgument(0), i.getArgument(1), i.getArgument(2), i.getArgument(3), i.getArgument(4)));
    }

    static Review review(String id, String book, String author, int rating, String at) {
        return new Review(id, book, author, rating, "text", at);
    }

    @Test
    void storesAValidReviewTrimmedAndStampedByTheClock() {
        var result = service.create("u5", "b1", 4, "  Loved it.  ");
        assertThat(result.ok()).isTrue();
        assertThat(result.problems()).isEmpty();
        verify(repo).add("b1", "u5", 4, "Loved it.", "2026-11-21T12:00:00Z");
    }

    @Test
    void withoutAViewerNothingElseIsChecked() {
        var result = service.create(null, "nope", 9, "");
        assertThat(result.ok()).isFalse();
        assertThat(result.problems()).hasSize(1);
        assertThat(result.problems().getFirst().field()).isNull();
        verify(repo, never()).add(any(), any(), anyInt(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5})
    void ratingsAtTheBoundsAreAccepted(int rating) {
        assertThat(service.create("u5", "b1", rating, "ok").ok()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6, -1})
    void ratingsOutsideOneToFiveAreRejected(int rating) {
        var result = service.create("u5", "b1", rating, "ok");
        assertThat(result.ok()).isFalse();
        assertThat(result.problems()).extracting(ReviewService.Problem::field).containsExactly(List.of("input", "rating"));
        verify(repo, never()).add(any(), any(), anyInt(), any(), any());
    }

    @Test
    void anUnknownBookIsReportedOnBookId() {
        var result = service.create("u5", "nope", 3, "ok");
        assertThat(result.problems()).extracting(ReviewService.Problem::field).containsExactly(List.of("input", "bookId"));
    }

    @Test
    void aBlankBodyIsRejectedAfterTrimming() {
        var result = service.create("u5", "b1", 3, " \t\n ");
        assertThat(result.problems()).extracting(ReviewService.Problem::field).containsExactly(List.of("input", "body"));
    }

    @Test
    void bodyLengthCountsAfterTrimming() {
        assertThat(service.create("u5", "b1", 3, "  " + "x".repeat(500) + "  ").ok()).isTrue();
        var tooLong = service.create("u6", "b1", 3, "x".repeat(501));
        assertThat(tooLong.problems()).extracting(ReviewService.Problem::message).containsExactly("At most 500 characters");
    }

    @Test
    void everyBrokenRuleIsReportedNotJustTheFirst() {
        var result = service.create("u5", "nope", 0, "");
        assertThat(result.problems()).extracting(ReviewService.Problem::field)
            .containsExactly(List.of("input", "bookId"), List.of("input", "rating"), List.of("input", "body"));
    }

    @Test
    void oneReviewPerUserPerBook() {
        when(repo.all()).thenReturn(List.of(review("r1", "b1", "u5", 5, "2026-01-01T00:00:00Z")));
        var again = service.create("u5", "b1", 4, "Second thoughts");
        assertThat(again.ok()).isFalse();
        assertThat(again.problems()).extracting(ReviewService.Problem::message).containsExactly("You already reviewed this book");
        assertThat(service.create("u6", "b1", 4, "Someone else").ok()).isTrue();
    }

    @Test
    void averageIsRoundedHalfUpToTwoDecimals() {
        when(repo.all()).thenReturn(List.of(
            review("r1", "b1", "u1", 5, "a"), review("r2", "b1", "u2", 5, "b"), review("r3", "b1", "u3", 4, "c"),
            review("r4", "b2", "u3", 1, "d")));
        assertThat(service.averageRating("b1")).isEqualTo(4.67);
        assertThat(service.averageRating("b2")).isEqualTo(1.0);
    }

    @Test
    void averageIsNullWithoutReviews() {
        assertThat(service.averageRating("b1")).isNull();
    }

    @Test
    void reviewsOfABookAreNewestFirstAndCapped() {
        when(repo.all()).thenReturn(List.of(
            review("r1", "b1", "u1", 5, "2026-01-01T00:00:00Z"), review("r2", "b1", "u2", 4, "2026-03-01T00:00:00Z"),
            review("r3", "b2", "u2", 4, "2026-04-01T00:00:00Z"), review("r16", "b1", "u3", 3, "2026-03-01T00:00:00Z"),
            review("r9", "b1", "u4", 3, "2026-02-01T00:00:00Z")));
        assertThat(service.reviewsOf("b1", 5)).extracting(Review::id).containsExactly("r16", "r2", "r9", "r1");
        assertThat(service.reviewsOf("b1", 2)).extracting(Review::id).containsExactly("r16", "r2");
        assertThat(service.reviewsOf("b1", -3)).isEmpty();
    }

    @Test
    void anUnknownReviewIsNotFound() {
        when(repo.find("zz")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.review("zz")).isInstanceOf(Problems.NotFound.class).hasMessageContaining("zz");
    }

    @Test
    void onlyTheAuthorDeletes() {
        when(repo.find("r1")).thenReturn(Optional.of(review("r1", "b1", "u1", 5, "a")));
        assertThatThrownBy(() -> service.delete("u2", "r1")).isInstanceOf(Problems.Forbidden.class);
        assertThatThrownBy(() -> service.delete(null, "r1")).isInstanceOf(Problems.Forbidden.class);
        verify(repo, never()).remove(any());
        assertThat(service.delete("u1", "r1")).isEqualTo("r1");
        verify(repo).remove("r1");
    }

    @Test
    void storedReviewsAreAnnouncedPerBook() {
        StepVerifier.create(service.added("b1").take(1))
            .then(() -> service.create("u5", "b1", 4, "Live!"))
            .assertNext(r -> assertThat(r.body()).isEqualTo("Live!"))
            .verifyComplete();
    }

    @Test
    void rejectedReviewsAreNotAnnounced() {
        StepVerifier.create(service.added(null).take(java.time.Duration.ofMillis(200)))
            .then(() -> service.create("u5", "b1", 9, "Bad rating"))
            .verifyComplete();
    }
}

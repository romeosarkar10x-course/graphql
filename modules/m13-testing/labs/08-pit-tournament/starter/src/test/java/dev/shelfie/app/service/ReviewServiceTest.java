package dev.shelfie.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * ReviewService with no Spring and no GraphQL: two mocked repositories and a fixed clock.
 * Read ReviewService's javadoc for the rules, then pin each one (and its edges) with a test.
 */
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
        when(repo.all()).thenReturn(List.of());
        // add() returns what it was given, under id r99
        when(repo.add(anyString(), anyString(), anyInt(), anyString(), anyString()))
            .thenAnswer(i -> new Review("r99", i.getArgument(0), i.getArgument(1), i.getArgument(2), i.getArgument(3), i.getArgument(4)));
    }

    @Test
    void storesAValidReview() {
        assertThat(service.create("u5", "b1", 4, "Loved it.").ok()).isTrue();
    }

    // TODO the rating bounds, the body rules, one review per user per book, every broken rule reported,
    //      averageRating's rounding and its null, the order of reviewsOf, who may delete.
}

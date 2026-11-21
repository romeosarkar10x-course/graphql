package dev.shelfie.filters;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Assume;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.BeforeContainer;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.domain.Specification;

/**
 * Property tests: 1,000 random filter and order combinations each, against an in-memory model of the
 * same 10,000 books. jqwik has no Spring extension here, so the context is started by hand, once.
 * Needs m08_04 loaded (bin/course catalog m08_04); run with mvn test.
 */
class FilterPropertiesTest {
    static ConfigurableApplicationContext context;
    static BookService service;
    static List<Book> all;

    @BeforeContainer
    static void start() {
        context = SpringApplication.run(FiltersApplication.class, "--server.port=0", "--spring.jpa.show-sql=false");
        service = context.getBean(BookService.class);
        all = context.getBean(BookRepository.class).findAll(Specification.unrestricted());
    }

    @AfterContainer
    static void stop() { context.close(); }

    // ---- the model: plain Java over the same rows ----

    static boolean matches(Book b, BookFilter f) {
        return (f.genres() == null || f.genres().contains(b.getGenre()))
            && (f.publishedFrom() == null || b.getPublished() >= f.publishedFrom())
            && (f.publishedTo() == null || b.getPublished() <= f.publishedTo())
            && (f.minRating() == null || b.getRating().compareTo(BigDecimal.valueOf(f.minRating())) >= 0)
            && (f.author() == null || b.getAuthor().equals(f.author()));
    }

    static Comparator<Book> order(BookOrder o) {
        Comparator<Book> key = switch (o.field()) {
            case TITLE -> Comparator.comparing(Book::getTitle);
            case PUBLISHED -> Comparator.comparingInt(Book::getPublished);
            case RATING -> Comparator.comparing(Book::getRating);
            case RELEVANCE -> throw new IllegalArgumentException();
        };
        if (o.direction() == BookOrder.Direction.DESC) key = key.reversed();
        return key.thenComparing(Book::getId);   // the tie-breaker is ascending in both directions
    }

    static Stream<Book> model(BookFilter f) { return all.stream().filter(b -> matches(b, f)); }

    static List<Long> ids(List<Book> books) { return books.stream().map(Book::getId).toList(); }

    // ---- properties ----

    @Property(tries = 1000)
    void theFirstNAreTheModelsFirstN(@ForAll("filters") BookFilter f, @ForAll("orders") BookOrder o, @ForAll("firsts") int first) {
        var expected = ids(model(f).sorted(order(o)).limit(first).toList());
        assertThat(ids(service.find(f, o, first))).isEqualTo(expected);
    }

    @Property(tries = 1000)
    void countIsEveryMatch(@ForAll("filters") BookFilter f) {
        assertThat(service.count(f)).isEqualTo(model(f).count());
    }

    /** Metamorphic: no model needed. Setting a missing publishedFrom can only remove books. */
    @Property(tries = 1000)
    void addingAFieldNeverGrowsTheResult(@ForAll("filters") BookFilter f, @ForAll("anyYear") int from) {
        Assume.that(f.publishedFrom() == null && (f.publishedTo() == null || from <= f.publishedTo()));
        var narrower = new BookFilter(f.genres(), from, f.publishedTo(), f.minRating(), f.author(), f.search());
        assertThat(service.count(narrower)).isLessThanOrEqualTo(service.count(f));
    }

    // ---- generators ----

    @Provide
    Arbitrary<BookFilter> filters() {
        Arbitrary<List<Genre>> genres = Arbitraries.of(Genre.class).list().uniqueElements().ofMaxSize(3).injectNull(0.5);
        Arbitrary<Double> rating = Arbitraries.integers().between(10, 50).map(i -> i / 10.0).injectNull(0.6);
        Arbitrary<String> author = Arbitraries.of("Mei Mensah", "Tomas Costa", "Omar Lindqvist", "Ines Tanaka", "Nobody Atall").injectNull(0.8);
        return Combinators.combine(genres, years(), years(), rating, author)
            .as((g, a, b, r, au) -> {
                Integer from = a, to = b;
                if (from != null && to != null && from > to) { from = b; to = a; }   // valid ranges only
                return new BookFilter(g, from, to, r, au, null);
            });
    }

    @Provide
    Arbitrary<Integer> years() { return Arbitraries.integers().between(1890, 2030).injectNull(0.5); }

    @Provide
    Arbitrary<Integer> anyYear() { return Arbitraries.integers().between(1890, 2030); }

    @Provide
    Arbitrary<BookOrder> orders() {
        return Combinators.combine(Arbitraries.of(BookOrder.Field.TITLE, BookOrder.Field.PUBLISHED, BookOrder.Field.RATING),
            Arbitraries.of(BookOrder.Direction.class)).as(BookOrder::new);
    }

    @Provide
    Arbitrary<Integer> firsts() { return Arbitraries.integers().between(0, BookService.MAX_FIRST); }
}

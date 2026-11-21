package dev.shelfie.filters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.lifecycle.AfterContainer;
import net.jqwik.api.lifecycle.BeforeContainer;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.domain.Specification;

/**
 * Property tests against an in-memory model of the same 10,000 books. jqwik has no Spring extension
 * here, so the context is started by hand, once. Needs m08_04 loaded (bin/course catalog m08_04).
 *
 * TODO (lab 8.4): at least 3 properties with @Property(tries = 1000), over filters that combine every
 * field but search, and over every order but RELEVANCE.
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

    /** An example to start from: every book returned for one genre has that genre. */
    @Property(tries = 100)
    void everyResultHasTheGenre(@ForAll("genres") Genre genre) {
        var filter = new BookFilter(List.of(genre), null, null, null, null, null);
        assertThat(service.find(filter, BookOrder.DEFAULT, 100)).allMatch(b -> b.getGenre() == genre);
    }

    @Provide
    Arbitrary<Genre> genres() { return Arbitraries.of(Genre.class); }
}

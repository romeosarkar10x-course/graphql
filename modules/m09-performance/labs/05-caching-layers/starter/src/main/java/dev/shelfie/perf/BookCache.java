package dev.shelfie.perf;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.shelfie.perf.Entities.Book;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** The entity cache: books by id, shared by every request, in this JVM only (Caffeine). */
@Component
class BookCache {
    private final Cache<Long, Book> cache = Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(Duration.ofMinutes(10)).build();
    private final BookRepository books;

    BookCache(BookRepository books) { this.books = books; }

    Map<Long, Book> getAll(Set<Long> ids) {
        return cache.getAll(ids, missing -> books.findAllById(java.util.List.copyOf(missing)).stream().collect(Collectors.toMap(b -> b.id, Function.identity())));
    }

    Book save(Book book) {
        return books.save(book);
    }

    java.util.Optional<Book> findById(long id) { return books.findById(id); }
}

package dev.shelfie.perf;

import dev.shelfie.perf.Entities.Book;
import dev.shelfie.perf.Entities.Club;
import dev.shelfie.perf.Entities.Reader;
import dev.shelfie.perf.Entities.Review;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.dataloader.DataLoader;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.execution.BatchLoaderRegistry;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

/**
 * Every field below the root, batched: one statement per field per level, however many parents.
 * Fields without arguments use @BatchMapping. Fields with arguments need the argument in the key,
 * so they get a DataLoader registered in BatchLoaderRegistry.
 */
@Controller
class BatchController {
    record MembersKey(long clubId, int first) {}
    record ReviewsKey(long readerId, int first) {}

    private final ReaderRepository readers;
    private final BookCache books;
    private final JdbcClient jdbc;

    BatchController(ReaderRepository readers, BookCache books, JdbcClient jdbc, BatchLoaderRegistry registry) {
        this.readers = readers; this.books = books; this.jdbc = jdbc;
        registry.<MembersKey, List<Reader>>forName("members").registerMappedBatchLoader((keys, env) -> Mono.fromCallable(() -> members(keys)));
        registry.<ReviewsKey, List<Review>>forName("reviews").registerMappedBatchLoader((keys, env) -> Mono.fromCallable(() -> reviews(keys)));
        registry.<Long, Book>forName("books").registerMappedBatchLoader((ids, env) -> Mono.fromCallable(() -> books.getAll(ids)));
    }

    @BatchMapping Map<Club, Reader> owner(List<Club> clubs) {
        var byId = byId(readers.findAllById(ids(clubs, c -> c.ownerId)), r -> r.id);
        return clubs.stream().collect(Collectors.toMap(c -> c, c -> byId.get(c.ownerId)));
    }

    @BatchMapping Map<Club, Book> currentBook(List<Club> clubs) {
        var byId = books.getAll(ids(clubs, c -> c.currentBookId));
        var out = new HashMap<Club, Book>();   // clubs without a current book stay out of the map: null
        clubs.stream().filter(c -> c.currentBookId != null).forEach(c -> out.put(c, byId.get(c.currentBookId)));
        return out;
    }

    @BatchMapping Map<Club, Integer> memberCount(List<Club> clubs) {
        Map<Long, Integer> counts = new HashMap<>();
        jdbc.sql("select club_id, count(*) from membership where club_id = any(?) group by club_id")
            .param(ids(clubs, c -> c.id).toArray(Long[]::new))
            .query((rs, n) -> counts.put(rs.getLong(1), rs.getInt(2))).list();
        return clubs.stream().collect(Collectors.toMap(c -> c, c -> counts.getOrDefault(c.id, 0)));
    }

    @SchemaMapping CompletableFuture<List<Reader>> members(Club club, @Argument int first, DataLoader<MembersKey, List<Reader>> members) {
        return members.load(new MembersKey(club.id, Math.clamp(first, 1, 100)));
    }

    @SchemaMapping CompletableFuture<List<Review>> reviews(Reader reader, @Argument int first, DataLoader<ReviewsKey, List<Review>> reviews) {
        return reviews.load(new ReviewsKey(reader.id, Math.clamp(first, 1, 100)));
    }

    /**
     * Chained loaders: the owner's reviews, then their books. The second load starts only when the first
     * completes, after graphql-java has dispatched the level. Without chaining enabled
     * (ChainingConfig) the books loader is never dispatched and the request hangs.
     */
    @SchemaMapping CompletableFuture<List<Book>> ownerPicks(Club club, @Argument int first,
            DataLoader<ReviewsKey, List<Review>> reviews, DataLoader<Long, Book> books) {
        return reviews.load(new ReviewsKey(club.ownerId, Math.clamp(first, 1, 10)))
            .thenCompose(rs -> books.loadMany(rs.stream().map(r -> r.bookId).toList()));
    }

    /** By id through the request's "books" loader: the same book is one object for the whole request. */
    @SchemaMapping CompletableFuture<Book> book(Review review, DataLoader<Long, Book> books) { return books.load(review.bookId); }

    @BatchMapping Map<Review, Reader> author(List<Review> reviews) {
        var byId = byId(readers.findAllById(ids(reviews, r -> r.authorId)), r -> r.id);
        return reviews.stream().collect(Collectors.toMap(r -> r, r -> byId.get(r.authorId)));
    }

    /** One statement for every (club, first) key: number the members per club, keep the first max(first). */
    Map<MembersKey, List<Reader>> members(Set<MembersKey> keys) {
        int max = keys.stream().mapToInt(MembersKey::first).max().orElse(0);
        record Row(long clubId, long rn, Reader reader) {}
        var rows = jdbc.sql("""
                select * from (select m.club_id, r.id, r.handle, r.name,
                  row_number() over (partition by m.club_id order by m.joined_at, r.id) as rn
                  from membership m join reader r on r.id = m.reader_id where m.club_id = any(?)) x
                where rn <= ? order by club_id, rn""")
            .param(keys.stream().map(MembersKey::clubId).distinct().toArray(Long[]::new)).param(max)
            .query((rs, n) -> { var r = new Reader(); r.id = rs.getLong("id"); r.handle = rs.getString("handle"); r.name = rs.getString("name");
                                return new Row(rs.getLong("club_id"), rs.getLong("rn"), r); }).list();
        return keys.stream().collect(Collectors.toMap(k -> k, k -> rows.stream()
            .filter(r -> r.clubId() == k.clubId() && r.rn() <= k.first()).map(Row::reader).toList()));
    }

    /** One statement for every (reader, first) key, newest first. */
    Map<ReviewsKey, List<Review>> reviews(Set<ReviewsKey> keys) {
        int max = keys.stream().mapToInt(ReviewsKey::first).max().orElse(0);
        record Row(long rn, Review review) {}
        var rows = jdbc.sql("""
                select * from (select v.*, row_number() over (partition by author_id order by created_at desc, id desc) as rn
                  from review v where author_id = any(?)) x where rn <= ? order by author_id, rn""")
            .param(keys.stream().map(ReviewsKey::readerId).distinct().toArray(Long[]::new)).param(max)
            .query((rs, n) -> { var v = new Review(); v.id = rs.getLong("id"); v.bookId = rs.getLong("book_id"); v.authorId = rs.getLong("author_id");
                                v.rating = rs.getInt("rating"); v.body = rs.getString("body"); v.createdAt = rs.getTimestamp("created_at").toInstant();
                                return new Row(rs.getLong("rn"), v); }).list();
        return keys.stream().collect(Collectors.toMap(k -> k, k -> rows.stream()
            .filter(r -> r.review().authorId == k.readerId() && r.rn() <= k.first()).map(Row::review).toList()));
    }

    static <T> Set<Long> ids(Collection<T> items, Function<T, Long> id) {
        return items.stream().map(id).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
    }

    static <T> Map<Long, T> byId(List<T> items, Function<T, Long> id) {
        return items.stream().collect(Collectors.toMap(id, t -> t));
    }
}

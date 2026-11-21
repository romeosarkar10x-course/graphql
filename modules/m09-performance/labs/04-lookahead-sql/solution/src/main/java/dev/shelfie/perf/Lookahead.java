package dev.shelfie.perf;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.multiset;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.selectCount;
import static org.jooq.impl.DSL.table;

import dev.shelfie.perf.Entities.Book;
import dev.shelfie.perf.Entities.Club;
import dev.shelfie.perf.Entities.Reader;
import dev.shelfie.perf.Entities.Review;
import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.SelectedField;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectField;
import org.jooq.Table;
import org.springframework.stereotype.Component;

/**
 * Reads the selection set of `clubs` and loads exactly the selected tree in one SQL statement: joins for
 * the to-one fields, a correlated count, and nested MULTISETs (PostgreSQL: JSON aggregation) for the lists.
 * Fields it does not prefetch (Review.author, say) still resolve through the loaders.
 */
@Component
class Lookahead {
    private final DSLContext sql;

    Lookahead(DSLContext sql) { this.sql = sql; }

    /** One statement can't serve the same list twice with different arguments (aliases): leave those to the loaders. */
    boolean canPrefetch(DataFetchingFieldSelectionSet s) {
        for (String f : List.of("owner", "currentBook", "ownerPicks", "memberCount", "members", "members/reviews", "members/reviews/book"))
            if (s.getFields(f).size() > 1) return false;
        return true;
    }

    List<Club> clubs(int first, DataFetchingFieldSelectionSet s) {
        Table<?> c = table(name("club")).as("c");   // every column is name-qualified: no code generation needed
        List<SelectField<?>> cols = new ArrayList<>(List.of(f("c", "id"), f("c", "name"), f("c", "owner_id"), f("c", "current_book_id")));
        if (s.contains("owner")) {
            cols.addAll(List.of(f("o", "handle").as("o_handle"), f("o", "name").as("o_name")));
        }
        if (s.contains("currentBook")) {
            cols.addAll(List.of(f("cb", "title").as("cb_title"), f("cb", "author").as("cb_author")));
        }
        Field<Result<Record>> picks = null, members = null;
        Field<Integer> count = null;
        if (s.contains("ownerPicks")) {
            picks = multiset(select(List.<SelectField<?>>of(f("pb", "id"), f("pb", "title"), f("pb", "author")))
                .from(table(name("review")).as("pv")).join(table(name("book")).as("pb")).on(f("pb", "id").eq(f("pv", "book_id")))
                .where(f("pv", "author_id").eq(f("c", "owner_id")))
                .orderBy(f("pv", "created_at").desc(), f("pv", "id").desc())
                .limit(arg(s.getFields("ownerPicks"), 3, 10))).as("picks");
            cols.add(picks);
        }
        if (s.contains("memberCount")) {
            count = field(selectCount().from(table(name("membership")).as("mc")).where(f("mc", "club_id").eq(f("c", "id")))).as("member_count");
            cols.add(count);
        }
        Field<Result<Record>> reviews = null;
        if (s.contains("members")) {
            List<SelectField<?>> memberCols = new ArrayList<>(List.of(f("r", "id"), f("r", "handle"), f("r", "name")));
            if (s.contains("members/reviews")) {
                List<SelectField<?>> reviewCols = new ArrayList<>(List.of(f("v", "id"), f("v", "rating"), f("v", "body"),
                    f("v", "book_id"), f("v", "author_id"), field(name("v", "created_at"), OffsetDateTime.class)));
                var reviewFrom = table(name("review")).as("v");
                Table<?> from = reviewFrom;
                if (s.contains("members/reviews/book")) {
                    reviewCols.addAll(List.of(f("b", "title").as("b_title"), f("b", "author").as("b_author")));
                    from = reviewFrom.join(table(name("book")).as("b")).on(f("b", "id").eq(f("v", "book_id")));
                }
                reviews = multiset(select(reviewCols).from(from).where(f("v", "author_id").eq(f("r", "id")))
                    .orderBy(f("v", "created_at").desc(), f("v", "id").desc())
                    .limit(arg(s.getFields("members/reviews"), 5, 100))).as("reviews");
                memberCols.add(reviews);
            }
            members = multiset(select(memberCols)
                .from(table(name("membership")).as("m")).join(table(name("reader")).as("r")).on(f("r", "id").eq(f("m", "reader_id")))
                .where(f("m", "club_id").eq(f("c", "id")))
                .orderBy(f("m", "joined_at"), f("r", "id"))
                .limit(arg(s.getFields("members"), 20, 100))).as("members");
            cols.add(members);
        }
        var select = sql.select(cols).from(c
            .leftJoin(table(name("reader")).as("o")).on(f("o", "id").eq(f("c", "owner_id")))
            .leftJoin(table(name("book")).as("cb")).on(f("cb", "id").eq(f("c", "current_book_id"))))
            .orderBy(f("c", "id")).limit(first);
        boolean owner = s.contains("owner"), current = s.contains("currentBook");
        final Field<Result<Record>> fPicks = picks, fMembers = members, fReviews = reviews;
        final Field<Integer> fCount = count;
        return select.fetch(rec -> {
            var club = new Club();
            club.id = rec.get(f("c", "id"), Long.class);
            club.name = rec.get(f("c", "name"), String.class);
            club.ownerId = rec.get(f("c", "owner_id"), Long.class);
            club.currentBookId = rec.get(f("c", "current_book_id"), Long.class);
            club.prefetched = true;
            if (owner) club.owner = reader(club.ownerId, rec.get("o_handle", String.class), rec.get("o_name", String.class));
            if (current && club.currentBookId != null) club.currentBook = book(club.currentBookId, rec.get("cb_title", String.class), rec.get("cb_author", String.class));
            if (fPicks != null) club.ownerPicks = rec.get(fPicks).map(p -> book(p.get(0, Long.class), p.get(1, String.class), p.get(2, String.class)));
            if (fCount != null) club.memberCount = rec.get(fCount);
            if (fMembers != null) club.members = rec.get(fMembers).map(m -> {
                var r = reader(m.get(0, Long.class), m.get(1, String.class), m.get(2, String.class));
                if (fReviews != null) r.reviews = review(m.get(fReviews));
                return r;
            });
            return club;
        });
    }

    static List<Review> review(Result<?> rows) {
        return rows.map(v -> {
            var r = new Review();
            r.id = v.get(0, Long.class); r.rating = v.get(1, Integer.class); r.body = v.get(2, String.class);
            r.bookId = v.get(3, Long.class); r.authorId = v.get(4, Long.class);
            r.createdAt = v.get(5, OffsetDateTime.class).toInstant();
            if (v.size() > 6) r.book = book(r.bookId, v.get(6, String.class), v.get(7, String.class));
            return r;
        });
    }

    static Field<Object> f(String table, String column) { return field(name(table, column)); }

    /** The `first` argument of a selected list field, clamped like the loaders clamp it. */
    static int arg(List<SelectedField> fields, int dflt, int max) {
        Object v = fields.getFirst().getArguments().get("first");
        return Math.clamp(v instanceof Integer i ? i : dflt, 1, max);
    }

    static Reader reader(Long id, String handle, String name) { var r = new Reader(); r.id = id; r.handle = handle; r.name = name; return r; }

    static Book book(Long id, String title, String author) { var b = new Book(); b.id = id; b.title = title; b.author = author; return b; }
}

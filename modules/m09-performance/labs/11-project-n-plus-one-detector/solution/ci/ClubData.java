import java.sql.DriverManager;

/**
 * Module 9's club dataset in PostgreSQL: deterministic, so statement counts and outputs are exact.
 *
 *   bin/course clubs <schema> [scale]        scale 1 (default): 100 clubs, 2,000 readers, 2,000 books,
 *                                            2,500 memberships, 20,000 reviews; scale 10 is ten times that
 *
 * Drops and recreates the schema. Every row is a pure function of its id:
 *   club c        owner 1 + (7c mod R), current book 1 + (31c mod B), none when c is a multiple of 10
 *   memberships   25 per club: reader 1 + ((53c + 79k) mod R) for k = 0..24, joined in k order
 *   review g      by reader 1 + (g mod R) (10 each), of book 1 + ((17g + 7 floor(g/R)) mod B), newest = highest g
 */
public class ClubData {
    public static void main(String[] args) throws Exception {
        if (args.length < 1 || !args[0].matches("[a-z][a-z0-9_]{0,30}")) {
            System.err.println("usage: ClubData <schema> [scale]   (schema: lower case, digits, _)");
            System.exit(2);
        }
        String schema = args[0];
        int scale = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        long clubs = 100L * scale, readers = 2_000L * scale, books = 2_000L * scale, reviews = 20_000L * scale;
        String url = System.getenv().getOrDefault("PGURL", "jdbc:postgresql://postgres:5432/shelfie");
        long t0 = System.nanoTime();
        try (var c = DriverManager.getConnection(url, "shelfie", "shelfie"); var s = c.createStatement()) {
            s.execute("drop schema if exists " + schema + " cascade");
            s.execute("create schema " + schema);
            s.execute("set search_path = " + schema);
            s.execute("""
                create function pick(words text[], g bigint, k int) returns text
                  language sql immutable as $$ select words[1 + ((g * k) % array_length(words, 1))] $$""");
            s.execute("""
                create table reader (id bigint primary key, handle text not null unique, name text not null);
                create table book (id bigint primary key, title text not null, author text not null);
                create table club (id bigint primary key, name text not null,
                  owner_id bigint not null references reader, current_book_id bigint references book);
                create table membership (club_id bigint not null references club, reader_id bigint not null references reader,
                  joined_at timestamptz not null, primary key (club_id, reader_id));
                create table review (id bigint primary key, book_id bigint not null references book,
                  author_id bigint not null references reader, rating int not null, body text not null,
                  created_at timestamptz not null)""");
            String first = "array['Ada','Kemi','Jonas','Mei','Ravi','Sofia','Tomas','Asha','Leila','Omar','Grace','Hugo','Ines','Dev','Noor','Pablo','Yuki']";
            String last = "array['Okafor','Lindqvist','Tanaka','Moreau','Iyer','Novak','Adeyemi','Costa','Brennan','Haddad','Kim','Fischer','Silva','Mensah','Ortiz','Petrov','Quinn','Rahman','Walsh']";
            String adj = "array['Silent','Hidden','Burning','Last','Glass','Winter','Paper','Iron','Velvet','Hollow','Distant','Midnight','Salt','Golden','Broken','Quiet','Wild','Lost','Northern','Crimson']";
            String noun = "array['Garden','River','House','Clock','Library','Harbour','Orchard','Mirror','Tower','Island','Letter','Forest','Station','Map','Lantern','Bridge','Archive','Engine','Market','Atlas']";
            s.execute("insert into reader select g, 'reader' || g, pick(%s, g, 3) || ' ' || pick(%s, g, 13) from generate_series(1, %d) g"
                .formatted(first, last, readers));
            s.execute("insert into book select g, 'The ' || pick(%s, g, 7) || ' ' || pick(%s, g, 11), pick(%s, g, 5) || ' ' || pick(%s, g, 17) from generate_series(1, %d) g"
                .formatted(adj, noun, first, last, books));
            s.execute(("insert into club select c, pick(%s, c, 3) || ' ' || pick(%s, c, 7) || ' Club', 1 + (7 * c) %% %d,"
                + " case when c %% 10 = 0 then null else 1 + (31 * c) %% %d end from generate_series(1, %d) c")
                .formatted(adj, noun, readers, books, clubs));
            s.execute(("insert into membership select c, 1 + (53 * c + 79 * k) %% %d, timestamptz '2026-01-01 00:00Z' + (c * 25 + k) * interval '1 hour'"
                + " from generate_series(1, %d) c, generate_series(0, 24) k").formatted(readers, clubs));
            s.execute(("insert into review select g, 1 + (17 * g + 7 * (g / %d)) %% %d, 1 + g %% %d, 1 + (7 * g) %% 5,"
                + " 'Review ' || g || ': ' || lower(pick(%s, g, 3)) || ' and ' || lower(pick(%s, g, 7)),"
                + " timestamptz '2026-01-01 00:00Z' + g * interval '1 minute' from generate_series(1, %d) g")
                .formatted(readers, books, readers, adj, adj, reviews));
            s.execute("""
                create index membership_club on membership (club_id, joined_at, reader_id);
                create index membership_reader on membership (reader_id);
                create index review_author on review (author_id, created_at desc, id desc);
                create index review_book on review (book_id);
                create sequence review_id start with %d;
                alter table review alter column id set default nextval('review_id')""".formatted(reviews + 1));
            s.execute("vacuum analyze");   // its own statement: VACUUM cannot run inside the implicit transaction
        }
        System.out.printf("%s: %d clubs, %d readers, %d books, %d memberships, %d reviews in %.1f s%n",
            schema, clubs, readers, books, clubs * 25, reviews, (System.nanoTime() - t0) / 1e9);
    }
}

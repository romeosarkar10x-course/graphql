import java.sql.DriverManager;

/**
 * Module 8's book catalogue in PostgreSQL: deterministic, so every expected output can be computed.
 *
 *   bin/course catalog <schema> [rows]        default 10000 rows; lab 8.2 uses 1000000
 *
 * Drops and recreates <schema>.book. Row g (1..rows) is a pure function of g, so the same id always has
 * the same title, author, genre, year and rating. Sort keys repeat on purpose (125 years, 41 ratings,
 * 460 titles), so a sort without a unique tie-breaker is not stable.
 */
public class Catalog {
    static final String WORDS = """
        create or replace function %1$s.pick(words text[], g bigint, k int) returns text
          language sql immutable as $$ select words[1 + ((g * k) %% array_length(words, 1))] $$;
        """;

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || !args[0].matches("[a-z][a-z0-9_]{0,30}")) {
            System.err.println("usage: Catalog <schema> [rows]   (schema: lower case, digits, _)");
            System.exit(2);
        }
        String schema = args[0];
        long rows = args.length > 1 ? Long.parseLong(args[1]) : 10_000;
        String url = System.getenv().getOrDefault("PGURL", "jdbc:postgresql://postgres:5432/shelfie");
        long t0 = System.nanoTime();
        try (var c = DriverManager.getConnection(url, "shelfie", "shelfie"); var s = c.createStatement()) {
            s.execute("drop schema if exists " + schema + " cascade");
            s.execute("create schema " + schema);
            s.execute(WORDS.formatted(schema));
            s.execute("""
                create table %1$s.book (
                  id        bigint primary key,
                  title     text not null,
                  author    text not null,
                  genre     text not null,
                  published int not null,
                  rating    numeric(2,1) not null,
                  pages     int not null,
                  blurb     text not null,
                  added_at  timestamptz not null
                )""".formatted(schema));
            s.execute("""
                insert into %1$s.book
                select g,
                  %1$s.pick(array['Silent','Hidden','Burning','Last','Glass','Winter','Paper','Iron','Velvet',
                    'Hollow','Distant','Midnight','Salt','Golden','Broken','Quiet','Wild','Lost','Northern','Crimson'], g, 7)
                  || ' ' || %1$s.pick(array['Garden','River','House','Clock','Library','Harbour','Orchard','Mirror',
                    'Tower','Island','Letter','Forest','Station','Map','Lantern','Bridge','Archive','Engine','Market',
                    'Cartographer','Season','Signal','Atlas'], g, 11),
                  %1$s.pick(array['Ada','Kemi','Jonas','Mei','Ravi','Sofia','Tomas','Asha','Leila','Omar','Grace',
                    'Hugo','Ines','Dev','Noor','Pablo','Yuki'], g, 3)
                  || ' ' || %1$s.pick(array['Okafor','Lindqvist','Tanaka','Moreau','Iyer','Novak','Adeyemi','Costa',
                    'Brennan','Haddad','Kim','Fischer','Silva','Mensah','Ortiz','Petrov','Quinn','Rahman','Walsh'], g, 13),
                  %1$s.pick(array['FANTASY','MYSTERY','SCIENCE_FICTION','ROMANCE','HISTORY','POETRY','THRILLER','MEMOIR'], g, 5),
                  1900 + ((g * 37) %% 125),
                  round(1 + ((g * 13) %% 41) / 10.0, 1),
                  80 + ((g * 29) %% 900),
                  'A ' || lower(%1$s.pick(array['Silent','Hidden','Burning','Last','Glass','Winter','Paper','Iron',
                    'Velvet','Hollow','Distant','Midnight','Salt','Golden','Broken','Quiet','Wild','Lost','Northern',
                    'Crimson'], g, 17)) || ' story of '
                  || %1$s.pick(array['grief','revenge','first love','a stolen map','a lighthouse keeper','two sisters',
                    'an unsolved murder','a failing orchard','a time machine','a travelling circus','exile',
                    'a dragon hunt','the last library'], g, 3)
                  || ' in ' || %1$s.pick(array['Lagos','Oslo','Kyoto','Lisbon','Mumbai','Prague','Accra','Lima',
                    'Dublin','Beirut','Seoul'], g, 17) || '.',
                  timestamptz '2026-01-01 00:00:00+00' + g * interval '1 minute'
                from generate_series(1, %2$d) g""".formatted(schema, rows));
            s.execute("create index on %s.book (published, id)".formatted(schema));
            s.execute("create index on %s.book (title, id)".formatted(schema));
            s.execute("create index on %s.book (added_at, id)".formatted(schema));
            s.execute("create index book_fts on %s.book using gin (to_tsvector('english', title || ' ' || blurb))".formatted(schema));
            s.execute("create sequence %1$s.book_id_seq start with %2$d owned by %1$s.book.id".formatted(schema, rows + 1));
            s.execute("alter table %1$s.book alter column id set default nextval('%1$s.book_id_seq')".formatted(schema));
            s.execute("vacuum analyze " + schema + ".book");   // what autovacuum would do: index-only scans work
        }
        System.out.printf("catalog: %d books in %s.book (%d ms)%n", rows, schema, (System.nanoTime() - t0) / 1_000_000);
    }
}

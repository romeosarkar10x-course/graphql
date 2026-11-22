import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seven days of Shelfie's client traffic, replayed in seconds (Module 16.2-16.3). Deterministic: the same seed sends
 * the same operations with the same timestamps, so "which fields went unused" has one right answer.
 *
 *   bin/course java modules/m16-production/contract/Traffic.java [url] [--seed 16] [--now 2026-11-15T00:00:00Z]
 *
 * Every request carries graphql-client-name, graphql-client-version and X-Simulated-Time (ms): the time it would
 * have been sent. 1,400 operations spread over the 7 days before --now, plus 60 from the 10 days before that (an
 * old iOS release that has since been replaced). Prints how many it sent per client and version.
 */
public class Traffic {
    record Op(String client, String version, String name, String document, int weight) {}

    static final List<Op> WEEK = List.of(
        new Op("web", "3.2.0", "ClubPage", "query ClubPage($id: ID!) { club(id: $id) { name members { name } currentBook { title authors reviews { rating body author { name } } } readingList { title year } } }", 30),
        new Op("web", "3.2.0", "BookPage", "query BookPage($id: ID!) { book(id: $id) { title authors year pages isbn13 averageRating reviews { rating body author { name handle } } similar(first: 4) { id title } } }", 30),
        new Op("web", "3.2.0", "Search", "query Search($text: String!) { search(text: $text) { id title authors } }", 10),
        new Op("ios", "2.9.1", "ClubScreen", "query ClubScreen($id: ID!) { club(id: $id) { name members { handle } currentBook { id title } } }", 15),
        new Op("ios", "2.9.1", "BookScreen", "query BookScreen($id: ID!) { book(id: $id) { title isbn13 averageRating } }", 15),
        new Op("ios", "2.8.0", "ClubList", "query ClubList { clubs { id name memberCount } }", 2),
        new Op("android", "1.4.0", "Shelf", "query Shelf { books(first: 5) { id title authors } }", 10),
        new Op("android", "1.4.0", "Rate", "mutation Rate($book: ID!) { addReview(bookId: $book, rating: 4, body: \"Loved it\") { id seq rating } }", 3),
        new Op("partner-bulk", "1.0.0", "Catalog", "query Catalog { books(first: 10) { id title isbn13 year pages } }", 5));
    static final List<Op> BEFORE = List.of(
        new Op("ios", "2.7.0", "BookScreen", "query BookScreen($id: ID!) { book(id: $id) { title isbn10 averageRating } }", 1));

    public static void main(String[] args) throws Exception {
        String url = "http://localhost:4000/graphql";
        long seed = 16;
        Instant now = Instant.parse("2026-11-15T00:00:00Z");
        for (int i = 0; i < args.length; i++)
            switch (args[i]) {
                case "--seed" -> seed = Long.parseLong(args[++i]);
                case "--now" -> now = Instant.parse(args[++i]);
                default -> url = args[i];
            }
        Random random = new Random(seed);
        long day = 86_400_000L, end = now.toEpochMilli();
        List<long[]> plan = new ArrayList<>();   // {time, index into WEEK (or -1 - index into BEFORE)}
        int total = WEEK.stream().mapToInt(Op::weight).sum();
        for (int n = 0; n < 1400; n++) {
            int pick = random.nextInt(total), i = 0;
            while (pick >= WEEK.get(i).weight()) pick -= WEEK.get(i++).weight();
            plan.add(new long[] {end - 1 - (long) (random.nextDouble() * 7 * day), i});
        }
        for (int n = 0; n < 60; n++) plan.add(new long[] {end - 7 * day - 1 - (long) (random.nextDouble() * 10 * day), -1});
        plan.sort((a, b) -> Long.compare(a[0], b[0]));
        HttpClient http = HttpClient.newHttpClient();
        java.util.Map<String, Integer> sent = new java.util.TreeMap<>();
        int failed = 0;
        String[] books = {"b1", "b2", "b3", "b4", "b5", "b6", "b7", "b8", "b9", "b10"}, words = {"the", "of", "sun", "day"};
        for (long[] p : plan) {
            Op op = p[1] >= 0 ? WEEK.get((int) p[1]) : BEFORE.get((int) (-1 - p[1]));
            String vars = op.document().contains("$id") && op.name().startsWith("Club")
                    ? "{\"id\":\"c" + (1 + random.nextInt(3)) + "\"}"
                    : op.document().contains("$id") ? "{\"id\":\"" + books[random.nextInt(10)] + "\"}"
                    : op.document().contains("$text") ? "{\"text\":\"" + words[random.nextInt(4)] + "\"}"
                    : op.document().contains("$book") ? "{\"book\":\"" + books[random.nextInt(10)] + "\"}" : "{}";
            String body = "{\"query\":\"" + op.document().replace("\"", "\\\"") + "\",\"operationName\":\"" + op.name()
                    + "\",\"variables\":" + vars + "}";
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
                    .header("graphql-client-name", op.client()).header("graphql-client-version", op.version())
                    .header("X-Simulated-Time", Long.toString(p[0])).POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200 || r.body().contains("\"errors\"")) failed++;
            sent.merge(op.client() + " " + op.version(), 1, Integer::sum);
        }
        sent.forEach((k, v) -> System.out.println(k + ": " + v));
        System.out.println("failed: " + failed);
    }
}

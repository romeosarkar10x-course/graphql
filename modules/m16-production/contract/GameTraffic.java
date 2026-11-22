import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * Traffic for the incident game day (lab 16.8): Shelfie's four clients as usual, one of them misbehaving, and
 * subscriptions that open and close all the time. Runs until --minutes have passed (default 6).
 *
 *   bin/course java modules/m16-production/contract/GameTraffic.java --seed N [--minutes 6] [--url http://localhost:4000/graphql]
 *
 * Spoiler: answers(seed) below is the game's answer key (the same function as the app's GameDay.answers).
 */
public class GameTraffic {
    static final List<String> SLOW = List.of("Club.members", "Club.currentBook", "Query.search", "Query.book");
    static final List<String> CLIENTS = List.of("web", "ios", "android", "partner-bulk");

    static Map<String, String> answers(long seed) {
        Random r = new Random(seed * 7919 + 16);
        String slow = SLOW.get(r.nextInt(SLOW.size()));
        String leak = CLIENTS.get(r.nextInt(CLIENTS.size()));
        String abusive;
        do { abusive = CLIENTS.get(r.nextInt(CLIENTS.size())); } while (abusive.equals(leak));
        return Map.of("slowField", slow, "leakingClient", leak, "abusiveClient", abusive);
    }

    record Op(String client, String version, String name, String document) {}
    static final List<Op> NORMAL = List.of(
        new Op("web", "3.2.0", "ClubPage", "query ClubPage { club(id: \"c1\") { name members { name } currentBook { title authors reviews { rating body } } readingList { title year } } }"),
        new Op("web", "3.2.0", "BookPage", "query BookPage { book(id: \"b2\") { title authors year averageRating reviews { rating body } similar(first: 4) { id title } } }"),
        new Op("web", "3.2.0", "Search", "query Search { search(text: \"the\") { id title } }"),
        new Op("ios", "2.9.1", "ClubScreen", "query ClubScreen { club(id: \"c2\") { name members { handle } currentBook { id title } } }"),
        new Op("ios", "2.9.1", "BookScreen", "query BookScreen { book(id: \"b6\") { title isbn13 averageRating } }"),
        new Op("android", "1.4.0", "Shelf", "query Shelf { books(first: 5) { id title authors } }"),
        new Op("android", "1.4.0", "ClubCard", "query ClubCard { club(id: \"c3\") { name currentBook { title } } }"),
        new Op("partner-bulk", "1.0.0", "Catalog", "query Catalog { books(first: 10) { id title isbn13 year pages } }"));
    static final String EXPENSIVE = "query Recommendations { books(first: 10) { title similar(first: 5) { title similar(first: 5) { title reviews { body } } } } }";

    public static void main(String[] a) throws Exception {
        long seed = -1;
        int minutes = 6;
        String url = "http://localhost:4000/graphql";
        for (int i = 0; i < a.length; i++)
            switch (a[i]) {
                case "--seed" -> seed = Long.parseLong(a[++i]);
                case "--minutes" -> minutes = Integer.parseInt(a[++i]);
                case "--url" -> url = a[++i];
                default -> throw new IllegalArgumentException("unknown option " + a[i]);
            }
        if (seed < 0) throw new IllegalArgumentException("--seed N is required");
        String abusive = answers(seed).get("abusiveClient");
        long end = System.currentTimeMillis() + minutes * 60_000L;
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        String target = url;
        List<Thread> threads = new ArrayList<>();
        // Normal traffic: about 25 operations a second, every client.
        for (int t = 0; t < 5; t++) {
            Random r = new Random(seed + t);
            threads.add(Thread.ofVirtual().start(() -> {
                while (System.currentTimeMillis() < end) {
                    Op op = NORMAL.get(r.nextInt(NORMAL.size()));
                    post(http, target, op.client(), op.version(), op.name(), op.document());
                    sleep(200);
                }
            }));
        }
        // The abusive client: the expensive recommendations query, 8 at a time, back to back.
        for (int t = 0; t < 8; t++)
            threads.add(Thread.ofVirtual().start(() -> {
                sleep(20_000);   // it starts a little after the game does
                while (System.currentTimeMillis() < end) post(http, target, abusive, "1.0.0", "Recommendations", EXPENSIVE);
            }));
        // Subscriptions: every client opens one, keeps it 2 s, closes it, again and again.
        String ws = url.replaceFirst("^http", "ws");
        for (String client : CLIENTS)
            threads.add(Thread.ofVirtual().start(() -> {
                while (System.currentTimeMillis() < end) {
                    try {
                        WebSocket s = http.newWebSocketBuilder().subprotocols("graphql-transport-ws")
                                .header("graphql-client-name", client).buildAsync(URI.create(ws), new WebSocket.Listener() {
                                    @Override public CompletionStage<?> onText(WebSocket w, CharSequence d, boolean last) {
                                        if (d.toString().contains("connection_ack"))
                                            w.sendText("{\"id\":\"1\",\"type\":\"subscribe\",\"payload\":{\"query\":\"subscription Live { reviewAdded { seq rating } }\"}}", true);
                                        w.request(1);
                                        return null;
                                    }
                                }).get(3, TimeUnit.SECONDS);
                        s.sendText("{\"type\":\"connection_init\"}", true);
                        sleep(2000);
                        s.sendText("{\"id\":\"1\",\"type\":\"complete\"}", true);
                        s.sendClose(WebSocket.NORMAL_CLOSURE, "bye").get(2, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        sleep(1000);
                    }
                }
            }));
        // A review every half second, so subscriptions have something to deliver.
        threads.add(Thread.ofVirtual().start(() -> {
            while (System.currentTimeMillis() < end) {
                post(http, target, "android", "1.4.0", "Rate", "mutation Rate { addReview(bookId: \"b3\", rating: 5, body: \"Again.\") { id } }");
                sleep(500);
            }
        }));
        System.out.println("game traffic running for " + minutes + " minutes (seed " + seed + ")");
        for (Thread t : threads) t.join();
        System.out.println("game traffic done");
    }

    static void post(HttpClient http, String url, String client, String version, String name, String doc) {
        String body = "{\"operationName\":\"" + name + "\",\"query\":\"" + doc.replace("\"", "\\\"") + "\"}";
        try {
            http.send(HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
                    .header("graphql-client-name", client).header("graphql-client-version", version)
                    .timeout(Duration.ofSeconds(20)).POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                    HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            sleep(200);
        }
    }

    static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
}

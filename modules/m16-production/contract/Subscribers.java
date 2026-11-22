// Many graphql-transport-ws subscribers that reconnect and resume, plus a writer: Module 16.4's dropped-message count.
//   java Subscribers.java [--clients 200] [--seconds 30] [--every 50] [--url ws://localhost:4000/graphql]
//                         [--reviews http://localhost:4100]
// Each client subscribes to  reviewAdded(after: $after) { seq }  and remembers the last seq it saw. When the server
// completes the subscription, sends an error, or the socket closes, it reconnects (every 100 ms until it gets in)
// and subscribes again with after = that last seq. The writer adds a review every --every ms once every client is
// subscribed, for --seconds. Two seconds after the last write it reports, as one JSON line:
//   {"clients", "written", "reconnects", "dropped", "duplicates", "unsubscribedAtEnd", "queries", "failedQueries"}
// dropped: for each client, written seqs from its first received seq to the last one written that it never got.
// Meanwhile 4 HTTP clients each POST  { book(id: "b1") { title } }  to the same URL (http) every 20 ms; a query
// fails when the connection fails or the answer is not 200 with data.book.title.
// It prints "all subscribed" when the writer starts, so a script can wait for that line before deploying.
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Subscribers {
  static final String DOC = "subscription Live($after: Int) { reviewAdded(after: $after) { seq } }";
  static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
  static String url = "ws://localhost:4000/graphql";
  static final AtomicInteger reconnects = new AtomicInteger();

  static class Client implements WebSocket.Listener {
    final Set<Integer> got = ConcurrentHashMap.newKeySet();
    final AtomicInteger duplicates = new AtomicInteger();
    volatile int last = -1, first = -1;
    volatile boolean subscribed, stopped;
    volatile WebSocket ws;
    final StringBuilder partial = new StringBuilder();
    final CountDownLatch firstSubscribe = new CountDownLatch(1);

    void connect() {
      while (!stopped) {
        try {
          partial.setLength(0);
          ws = HTTP.newWebSocketBuilder().subprotocols("graphql-transport-ws")
              .buildAsync(URI.create(url), this).get(3, TimeUnit.SECONDS);
          ws.sendText("{\"type\":\"connection_init\"}", true);
          return;
        } catch (Exception e) {
          sleep(100);
        }
      }
    }

    void lost() {
      if (stopped) return;
      subscribed = false;
      reconnects.incrementAndGet();
      Thread.ofVirtual().start(() -> { sleep(100); connect(); });
    }

    @Override public CompletionStage<?> onText(WebSocket w, CharSequence data, boolean lastPart) {
      partial.append(data);
      w.request(1);
      if (!lastPart) return null;
      String msg = partial.toString();
      partial.setLength(0);
      if (msg.contains("\"connection_ack\"")) {
        String vars = last < 0 ? "{}" : "{\"after\":" + last + "}";
        w.sendText("{\"id\":\"1\",\"type\":\"subscribe\",\"payload\":{\"query\":\"" + DOC.replace("\"", "\\\"")
            + "\",\"variables\":" + vars + "}}", true);
        subscribed = true;
        firstSubscribe.countDown();
      } else if (msg.contains("\"type\":\"next\"")) {
        int i = msg.indexOf("\"seq\":") + 6, j = i;
        while (j < msg.length() && Character.isDigit(msg.charAt(j))) j++;
        int seq = Integer.parseInt(msg.substring(i, j));
        if (!got.add(seq)) duplicates.incrementAndGet();
        if (first < 0) first = seq;
        last = Math.max(last, seq);
      } else if (msg.contains("\"type\":\"ping\"")) {
        w.sendText("{\"type\":\"pong\"}", true);
      } else if (msg.contains("\"type\":\"complete\"") || msg.contains("\"type\":\"error\"")) {
        ws = null;   // this socket is done; its close must not reconnect a second time
        w.sendClose(WebSocket.NORMAL_CLOSURE, "resubscribe");
        lost();
      }
      return null;
    }

    @Override public CompletionStage<?> onClose(WebSocket w, int code, String reason) {
      if (w == ws) { ws = null; lost(); }
      return null;
    }

    @Override public void onError(WebSocket w, Throwable error) {
      if (w == ws) { ws = null; lost(); }
    }
  }

  public static void main(String[] a) throws Exception {
    int clients = 200, seconds = 30, every = 50;
    String reviews = "http://localhost:4100";
    for (int i = 0; i < a.length; i++)
      switch (a[i]) {
        case "--clients" -> clients = Integer.parseInt(a[++i]);
        case "--seconds" -> seconds = Integer.parseInt(a[++i]);
        case "--every" -> every = Integer.parseInt(a[++i]);
        case "--url" -> url = a[++i];
        case "--reviews" -> reviews = a[++i];
        default -> throw new IllegalArgumentException("unknown option " + a[i]);
      }
    List<Client> all = new ArrayList<>();
    for (int i = 0; i < clients; i++) {
      Client c = new Client();
      all.add(c);
      Thread.ofVirtual().start(c::connect);
    }
    for (Client c : all) if (!c.firstSubscribe.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("a client never subscribed");
    Thread.sleep(500);
    reconnects.set(0);
    System.out.println("all subscribed");
    System.out.flush();
    var queries = new AtomicInteger();
    var failedQueries = new AtomicInteger();
    var stopQueries = new AtomicBoolean();
    String httpUrl = url.replaceFirst("^ws", "http");
    var query = HttpRequest.newBuilder(URI.create(httpUrl)).header("Content-Type", "application/json").timeout(Duration.ofSeconds(5))
        .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"{ book(id: \\\"b1\\\") { title } }\"}")).build();
    List<Thread> queriers = new ArrayList<>();
    for (int q = 0; q < 4; q++)
      queriers.add(Thread.ofVirtual().start(() -> {
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        while (!stopQueries.get()) {
          queries.incrementAndGet();
          try {
            var r = http.send(query, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200 || !r.body().contains("\"title\"")) failedQueries.incrementAndGet();
          } catch (Exception e) {
            failedQueries.incrementAndGet();
          }
          sleep(20);
        }
      }));
    List<Integer> written = new ArrayList<>();
    long end = System.currentTimeMillis() + seconds * 1000L;
    var post = HttpRequest.newBuilder(URI.create(reviews + "/reviews")).header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("{\"bookId\":\"b2\",\"authorId\":\"u6\",\"rating\":4,\"body\":\"Still thinking about the statues.\"}")).build();
    while (System.currentTimeMillis() < end) {
      String body = HTTP.send(post, HttpResponse.BodyHandlers.ofString()).body();
      int i = body.indexOf("\"seq\":") + 6, j = i;
      while (j < body.length() && Character.isDigit(body.charAt(j))) j++;
      written.add(Integer.parseInt(body.substring(i, j)));
      Thread.sleep(every);
    }
    Thread.sleep(2000);
    stopQueries.set(true);
    for (Thread t : queriers) t.join();
    int maxWritten = written.getLast();
    long dropped = 0, dups = 0, unsubscribed = 0;
    for (Client c : all) {
      c.stopped = true;
      dups += c.duplicates.get();
      if (!c.subscribed) unsubscribed++;
      int from = c.first < 0 ? written.getFirst() : c.first;
      for (int s : written) if (s >= from && s <= maxWritten && !c.got.contains(s)) dropped++;
      WebSocket w = c.ws;
      if (w != null) w.abort();
    }
    System.out.printf("{\"clients\":%d,\"written\":%d,\"reconnects\":%d,\"dropped\":%d,\"duplicates\":%d,\"unsubscribedAtEnd\":%d,\"queries\":%d,\"failedQueries\":%d}%n",
        clients, written.size(), reconnects.get(), dropped, dups, unsubscribed, queries.get(), failedQueries.get());
    System.exit(0);
  }

  static void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
}

// Lab 11.5's experiment: N clients keeping up with one chat room, by polling or by subscription.
//   bin/course java modules/m11-realtime/contract/Load.java poll|subscribe|idle [--url http://localhost:4000/graphql]
//        [--clients 500] [--seconds 20] [--interval 2000] [--room r1] [--send-every 1000] [--pid <server pid>]
// While it runs it sends a message to the room every --send-every milliseconds. At the end it prints the HTTP requests
// made, the messages each client saw, the delay from sentAt to "seen" (median, p95, max) and, with --pid,
// the server's CPU time from /proc. Run it in the server's container so the clocks agree.
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;

public class Load {
  static final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
  static final AtomicLong requests = new AtomicLong(), clientRequests = new AtomicLong(), seen = new AtomicLong(), failures = new AtomicLong();
  static final List<Long> delays = Collections.synchronizedList(new ArrayList<>());
  static final Pattern MSG = Pattern.compile("\"id\":\"m(\\d+)\"[^}]*?\"sentAt\":\"([^\"]+)\"");

  public static void main(String[] a) throws Exception {
    String mode = a[0], url = "http://localhost:4000/graphql", room = "r1";
    int clients = 500, seconds = 20, interval = 2000, every = 1000;
    String pid = null;
    for (int i = 1; i < a.length; i++)
      switch (a[i]) {
        case "--url" -> url = a[++i];
        case "--clients" -> clients = Integer.parseInt(a[++i]);
        case "--seconds" -> seconds = Integer.parseInt(a[++i]);
        case "--interval" -> interval = Integer.parseInt(a[++i]);
        case "--room" -> room = a[++i];
        case "--send-every" -> every = Integer.parseInt(a[++i]);
        case "--pid" -> pid = a[++i];
        default -> throw new IllegalArgumentException(a[i]);
      }
    long cpu0 = cpu(pid);
    long end = System.currentTimeMillis() + seconds * 1000L;
    String startAfter = latest(url, room);
    clientRequests.set(0);
    var workers = new ArrayList<Thread>();
    final String u = url, r = room;
    final int iv = interval;
    if (mode.equals("idle")) {
      // no clients: the server's own cost of sending the messages (the baseline)
    } else if (mode.equals("poll")) {
      for (int c = 0; c < clients; c++) {
        long offset = ThreadLocalRandom.current().nextLong(iv);
        workers.add(Thread.ofVirtual().start(() -> poll(u, r, startAfter, iv, offset, end)));
      }
    } else {
      var ready = new CountDownLatch(clients);
      for (int c = 0; c < clients; c++) workers.add(Thread.ofVirtual().start(() -> subscribe(u, r, end, ready)));
      ready.await(30, TimeUnit.SECONDS);
      if (pid != null) System.out.printf("  server CPU to open %d subscriptions: %.1f s%n", clients, (cpu(pid) - cpu0) / 100.0);
    }
    long sent = 0;
    while (System.currentTimeMillis() < end - 2000) {   // the last 2 s let the last poll catch up
      clientRequests.decrementAndGet();                 // the sender's requests are not the clients'
      gql(url, "mutation { sendMessage(roomId: \"" + room + "\", author: \"load\", body: \"tick " + sent + "\") { id } }");
      sent++;
      Thread.sleep(every);
    }
    if (mode.equals("idle")) Thread.sleep(Math.max(0, end - System.currentTimeMillis()));
    for (Thread t : workers) t.join();
    long sends = sent;
    List<Long> d = new ArrayList<>(delays);
    Collections.sort(d);
    System.out.printf("mode %s: %d clients, %d s, %d messages sent%n", mode, clients, seconds, sends);
    System.out.printf("  HTTP requests by clients: %d (%.0f a second), WebSockets: %d, failed: %d%n", clientRequests.get(),
        clientRequests.get() / (double) seconds, mode.equals("subscribe") ? clients : 0, failures.get());
    System.out.printf("  messages seen: %d of %d (%.1f%%)%n", seen.get(), sends * clients, 100.0 * seen.get() / Math.max(1, sends * clients));
    if (!d.isEmpty())
      System.out.printf("  delay sentAt -> seen: median %d ms, p95 %d ms, max %d ms%n", d.get(d.size() / 2), d.get((int) (d.size() * 0.95)), d.get(d.size() - 1));
    if (pid != null) System.out.printf("  server CPU: %.1f s%n", (cpu(pid) - cpu0) / 100.0);
    System.exit(0);
  }

  static void poll(String url, String room, String after, int interval, long offset, long end) {
    String last = after;
    try {
      Thread.sleep(offset);
      while (System.currentTimeMillis() < end) {
        String q = "{ messages(roomId: \"" + room + "\", after: \"" + last + "\", last: 1000) { id sentAt } }";
        String body = gql(url, q);
        for (Matcher m = MSG.matcher(body); m.find(); ) { record(m.group(2)); last = "m" + m.group(1); }
        Thread.sleep(interval);
      }
    } catch (Exception e) {
      failures.incrementAndGet();
    }
  }

  static void subscribe(String url, String room, long end, CountDownLatch ready) {
    try {
      var done = new CompletableFuture<Void>();
      WebSocket ws = http.newWebSocketBuilder().subprotocols("graphql-transport-ws").buildAsync(URI.create(url.replaceFirst("^http", "ws")),
          new WebSocket.Listener() {
            final StringBuilder buf = new StringBuilder();
            public CompletionStage<?> onText(WebSocket w, CharSequence data, boolean lastPart) {
              buf.append(data);
              if (lastPart) {
                String m = buf.toString();
                buf.setLength(0);
                if (m.contains("connection_ack")) {
                  w.sendText("{\"id\":\"1\",\"type\":\"subscribe\",\"payload\":{\"query\":\"subscription { messageSent(roomId: \\\"" + room + "\\\") { id sentAt } }\"}}", true);
                  ready.countDown();
                } else if (m.contains("\"ping\"")) w.sendText("{\"type\":\"pong\"}", true);
                else for (Matcher x = MSG.matcher(m); x.find(); ) record(x.group(2));
              }
              w.request(1);
              return null;
            }
            public CompletionStage<?> onClose(WebSocket w, int code, String reason) { done.complete(null); return null; }
            public void onError(WebSocket w, Throwable e) { failures.incrementAndGet(); done.complete(null); }
          }).get(10, TimeUnit.SECONDS);
      ws.sendText("{\"type\":\"connection_init\"}", true);
      Thread.sleep(Math.max(0, end - System.currentTimeMillis()));
      ws.abort();
    } catch (Exception e) {
      failures.incrementAndGet();
      ready.countDown();
    }
  }

  static void record(String sentAt) {
    seen.incrementAndGet();
    delays.add(Duration.between(Instant.parse(sentAt), Instant.now()).toMillis());
  }

  static String latest(String url, String room) throws Exception {
    String body = gql(url, "{ messages(roomId: \"" + room + "\", last: 1) { id sentAt } }");
    Matcher m = MSG.matcher(body);
    return m.find() ? "m" + m.group(1) : "m0";
  }

  static String gql(String url, String query) throws Exception {
    requests.incrementAndGet();
    clientRequests.incrementAndGet();
    String body = "{\"query\":\"" + query.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
    var res = http.send(HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    if (res.statusCode() != 200) failures.incrementAndGet();
    return res.body();
  }

  static long cpu(String pid) throws Exception {
    if (pid == null) return 0;
    String[] f = Files.readString(Path.of("/proc/" + pid + "/stat")).replaceAll(".*\\) ", "").split(" ");
    return Long.parseLong(f[11]) + Long.parseLong(f[12]);   // utime + stime, in clock ticks (100 a second)
  }
}

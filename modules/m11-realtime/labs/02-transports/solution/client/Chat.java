// Lab 11.2: a chat client that survives a dropped connection, over either transport.
//   bin/course java client/Chat.java <http url> <room> [--transport ws|sse] [--seconds N]
// stdout: one line per new message, "<id> <author>: <body>", each exactly once and in order.
// stderr: connection events ("connected ws", "dropped: ...", "reconnecting in 400 ms", "filled 3").
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class Chat {
  static final JsonMapper json = JsonMapper.builder().build();
  // HTTP/1.1: plain chunked streaming, no h2c upgrade attempt on http:// URLs.
  static final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(2)).build();
  static final String SUB = "subscription($r: ID!) { messageSent(roomId: $r) { id author body } }";
  static final String GAP = "query($r: ID!, $after: ID) { messages(roomId: $r, after: $after, last: 1000) { id author body } }";

  static String url, room;
  static long lastSeq = -1;                 // the newest message printed; -1 before the first connection
  static final List<JsonNode> held = new ArrayList<>(); // live messages held back while the gap is filled
  static boolean filling;

  public static void main(String[] a) throws Exception {
    url = a[0];
    room = a[1];
    String transport = "ws";
    int seconds = 60;
    for (int i = 2; i < a.length; i++)
      switch (a[i]) {
        case "--transport" -> transport = a[++i];
        case "--seconds" -> seconds = Integer.parseInt(a[++i]);
        default -> throw new IllegalArgumentException(a[i]);
      }
    long end = System.nanoTime() + seconds * 1_000_000_000L;
    long backoff = 200;
    while (System.nanoTime() < end) {
      var dropped = new CompletableFuture<String>();
      var connected = new CompletableFuture<Void>();
      // Hold live events from the moment the new subscription can deliver them: one printed before the
      // gap is filled would move lastSeq past the messages the gap query is about to return.
      synchronized (Chat.class) { if (lastSeq >= 0) filling = true; }
      Thread.ofVirtual().start(transport.equals("sse") ? () -> sse(connected, dropped) : () -> ws(connected, dropped));
      try {
        CompletableFuture.anyOf(connected, dropped).get(3, TimeUnit.SECONDS);
        if (!connected.isDone()) throw new ExecutionException(null);
      } catch (TimeoutException | ExecutionException e) {
        log("dropped: " + (dropped.isDone() ? dropped.get() : "no connection"));
        pause(backoff);
        backoff = Math.min(backoff * 2, 3000);
        continue;
      }
      log("connected " + transport);
      fillGap();                            // subscribed first, then fetched: nothing falls in between
      backoff = 200;
      try {
        log("dropped: " + dropped.get(Math.max(1, (end - System.nanoTime()) / 1_000_000), TimeUnit.MILLISECONDS));
      } catch (TimeoutException e) {
        break;                              // --seconds is over
      }
      if (System.nanoTime() >= end) break;
      pause(backoff);
      backoff = Math.min(backoff * 2, 3000);
    }
    System.exit(0);
  }

  /** Exponential backoff with jitter, so a thousand clients cut off together do not return in lockstep. */
  static void pause(long backoff) throws InterruptedException {
    long wait = backoff + ThreadLocalRandom.current().nextLong(backoff / 2 + 1);
    log("reconnecting in " + wait + " ms");
    Thread.sleep(wait);
  }

  /** The first connection starts from "now"; a reconnection asks for everything after the last message seen. */
  static void fillGap() throws Exception {
    synchronized (Chat.class) {
      if (lastSeq < 0) { lastSeq = latestSeq(); return; }
    }
    JsonNode gap = query(GAP, Map.of("r", room, "after", "m" + lastSeq)).path("data").path("messages");
    synchronized (Chat.class) {
      int n = 0;
      for (JsonNode m : gap) n += print(m) ? 1 : 0;
      held.sort(Comparator.comparingLong(Chat::seq));
      for (JsonNode m : held) print(m);
      held.clear();
      filling = false;
      log("filled " + n);
    }
  }

  static long latestSeq() throws Exception {
    JsonNode last = query(GAP, Map.of("r", room)).path("data").path("messages");
    return last.isEmpty() ? 0 : seq(last.get(last.size() - 1));
  }

  static synchronized void live(JsonNode m) throws Exception {
    log("live " + m.path("id").asString());
    if (filling || lastSeq < 0) held.add(m);
    else print(m);
  }

  static boolean print(JsonNode m) {
    if (seq(m) <= lastSeq) return false;    // already shown: the gap query and the live stream overlap
    lastSeq = seq(m);
    System.out.println(m.path("id").asString() + " " + m.path("author").asString() + ": " + m.path("body").asString());
    System.out.flush();
    return true;
  }

  static long seq(JsonNode m) { return Long.parseLong(m.path("id").asString().substring(1)); }

  static JsonNode query(String q, Map<String, Object> vars) throws Exception {
    var body = json.writeValueAsString(Map.of("query", q, "variables", vars));
    var res = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(3)).header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    return json.readTree(res.body());
  }

  static String subscribeBody() {
    return json.writeValueAsString(Map.of("query", SUB, "variables", Map.of("r", room)));
  }

  /** graphql-transport-ws: connection_init, wait for connection_ack, subscribe; answer pings. */
  static void ws(CompletableFuture<Void> connected, CompletableFuture<String> dropped) {
    try {
      var acked = new CompletableFuture<Void>();
      WebSocket socket = http.newWebSocketBuilder().subprotocols("graphql-transport-ws")
          .buildAsync(URI.create(url.replaceFirst("^http", "ws")), new WebSocket.Listener() {
            final StringBuilder buf = new StringBuilder();

            public CompletionStage<?> onText(WebSocket w, CharSequence d, boolean last) {
              buf.append(d);
              if (last) {
                JsonNode f = json.readTree(buf.toString());
                buf.setLength(0);
                switch (f.path("type").asString()) {
                  case "connection_ack" -> acked.complete(null);
                  case "ping" -> w.sendText("{\"type\":\"pong\"}", true);
                  case "next" -> { try { live(f.path("payload").path("data").path("messageSent")); } catch (Exception e) { dropped.complete(e.toString()); } }
                  case "error", "complete" -> dropped.complete("server ended the subscription: " + f);
                  default -> {}
                }
              }
              w.request(1);
              return null;
            }

            public CompletionStage<?> onClose(WebSocket w, int code, String reason) { dropped.complete("closed " + code); return null; }

            public void onError(WebSocket w, Throwable e) { dropped.complete(String.valueOf(e)); }
          }).get(2, TimeUnit.SECONDS);
      socket.sendText("{\"type\":\"connection_init\"}", true);
      acked.get(2, TimeUnit.SECONDS);
      socket.sendText("{\"id\":\"1\",\"type\":\"subscribe\",\"payload\":" + subscribeBody() + "}", true);
      Thread.sleep(100); // the server registers the subscription before it is used for the gap
      connected.complete(null);
      dropped.whenComplete((r, e) -> socket.abort());
    } catch (Exception e) {
      dropped.complete(String.valueOf(e));
    }
  }

  /** graphql-sse (distinct connections mode): one POST per subscription, events arrive as "event:"/"data:" lines. */
  static void sse(CompletableFuture<Void> connected, CompletableFuture<String> dropped) {
    try {
      var pending = http.sendAsync(HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
          .header("Accept", "text/event-stream").POST(HttpRequest.BodyPublishers.ofString(subscribeBody())).build(),
          HttpResponse.BodyHandlers.ofLines());
      // Spring commits the response headers only with the first event or keep-alive, so "headers received"
      // cannot mean "subscribed". Give the server 300 ms to start the subscription instead.
      Thread.sleep(300);
      if (pending.isCompletedExceptionally()) { dropped.complete(String.valueOf(pending.exceptionNow())); return; }
      connected.complete(null);
      var res = pending.get();
      if (res.statusCode() != 200) { dropped.complete("status " + res.statusCode()); return; }
      String event = null;
      for (String line : (Iterable<String>) res.body()::iterator) {
        if (line.startsWith("event:")) event = line.substring(6).trim();
        else if (line.startsWith("data:") && "next".equals(event)) live(json.readTree(line.substring(5)).path("data").path("messageSent"));
        else if ("complete".equals(event)) { dropped.complete("server completed the stream"); return; }
      }
      dropped.complete("stream ended");
    } catch (Exception e) {
      dropped.complete(String.valueOf(e));
    }
  }

  static void log(String s) { System.err.println(s); }
}

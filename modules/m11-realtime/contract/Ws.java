// A graphql-transport-ws client for Module 11's tests and experiments. JDK only.
//   java Ws.java <ws url> <document> [--seconds N] [--count N] [--vars JSON] [--read-rate N]
// Prints one line per event, prefixed with milliseconds since the subscribe message was sent:
//   ready                      after connection_ack, when the subscribe message has been sent
//   next <payload JSON>        each result
//   error <payload JSON>       a graphql-transport-ws error message (the operation is over)
//   complete                   the server completed the operation
//   ping                       a server keep-alive ping (answered with pong)
//   closed <code> <reason>     the socket closed
//   end                        --seconds elapsed or --count next messages received
// --read-rate N reads at most N frames a second (a slow client); the default reads as fast as it can.
import java.net.URI;
import java.net.http.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Ws {
  static long t0 = System.nanoTime();

  static synchronized void out(String s) {
    System.out.printf("%6d %s%n", (System.nanoTime() - t0) / 1_000_000, s);
    System.out.flush();
  }

  public static void main(String[] a) throws Exception {
    String url = a[0], doc = a[1], vars = "{}";
    int seconds = 10, count = -1, rate = -1;
    for (int i = 2; i < a.length; i++)
      switch (a[i]) {
        case "--seconds" -> seconds = Integer.parseInt(a[++i]);
        case "--count" -> count = Integer.parseInt(a[++i]);
        case "--vars" -> vars = a[++i];
        case "--read-rate" -> rate = Integer.parseInt(a[++i]);
        default -> throw new IllegalArgumentException("unknown option " + a[i]);
      }
    var done = new CompletableFuture<Void>();
    var acked = new CompletableFuture<Void>();
    var nexts = new AtomicInteger();
    var sched = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r); t.setDaemon(true); return t; });
    final int max = count, perSecond = rate;
    WebSocket ws;
    try {
      ws = HttpClient.newHttpClient().newWebSocketBuilder().subprotocols("graphql-transport-ws")
          .buildAsync(URI.create(url), new WebSocket.Listener() {
            final StringBuilder buf = new StringBuilder();

            public void onOpen(WebSocket w) { w.request(1); }

            public CompletionStage<?> onText(WebSocket w, CharSequence d, boolean last) {
              buf.append(d);
              if (last) {
                String m = buf.toString();
                buf.setLength(0);
                String type = m.replaceAll("(?s).*\"type\":\"([a-z_]+)\".*", "$1");
                String payload = m.replaceAll("(?s).*\"payload\":(.*),\"type\":.*", "$1");
                switch (type) {
                  case "connection_ack" -> acked.complete(null);
                  case "next" -> { out("next " + payload); if (nexts.incrementAndGet() == max) done.complete(null); }
                  case "error" -> { out("error " + payload); done.complete(null); }
                  case "complete" -> { out("complete"); done.complete(null); }
                  case "ping" -> { out("ping"); w.sendText("{\"type\":\"pong\"}", true); }
                  default -> out("frame " + m);
                }
              }
              if (perSecond > 0) sched.schedule(() -> w.request(1), 1000 / perSecond, TimeUnit.MILLISECONDS);
              else w.request(1);
              return null;
            }

            public CompletionStage<?> onClose(WebSocket w, int code, String reason) {
              out("closed " + code + " " + reason);
              acked.complete(null);
              done.complete(null);
              return null;
            }

            public void onError(WebSocket w, Throwable e) {
              out("closed -1 " + e);
              acked.complete(null);
              done.complete(null);
            }
          }).get(5, TimeUnit.SECONDS);
    } catch (ExecutionException e) {
      out("closed -1 " + e.getCause());
      return;
    }
    ws.sendText("{\"type\":\"connection_init\"}", true);
    acked.get(5, TimeUnit.SECONDS);
    if (done.isDone()) return;
    String q = doc.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    ws.sendText("{\"id\":\"1\",\"type\":\"subscribe\",\"payload\":{\"query\":\"" + q + "\",\"variables\":" + vars + "}}", true);
    t0 = System.nanoTime();
    out("ready");
    try {
      done.get(seconds, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      // fall through
    }
    if (!done.isDone() || nexts.get() == max) out("end");
    ws.abort();
    System.exit(0);
  }
}

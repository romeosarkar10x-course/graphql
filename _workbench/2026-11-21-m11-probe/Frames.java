// graphql-transport-ws frame dumper. JDK only.
//   java Frames.java <ws url> <subscription document> [seconds] [close-after-n-next]
// Prints every frame received, prefixed with milliseconds since connect.
import java.net.URI;
import java.net.http.*;
import java.util.concurrent.*;

public class Frames {
  public static void main(String[] a) throws Exception {
    String url = a[0], doc = a[1];
    int secs = a.length > 2 ? Integer.parseInt(a[2]) : 5, closeAfter = a.length > 3 ? Integer.parseInt(a[3]) : -1;
    long t0 = System.nanoTime();
    var done = new CompletableFuture<Void>();
    var acked = new CompletableFuture<Void>();
    int[] nexts = {0};
    WebSocket ws = HttpClient.newHttpClient().newWebSocketBuilder().subprotocols("graphql-transport-ws")
        .buildAsync(URI.create(url), new WebSocket.Listener() {
          StringBuilder buf = new StringBuilder();
          public CompletionStage<?> onText(WebSocket w, CharSequence d, boolean last) {
            buf.append(d);
            if (last) {
              String m = buf.toString(); buf.setLength(0);
              System.out.printf("%5d ms  %s%n", (System.nanoTime() - t0) / 1_000_000, m);
              if (m.contains("connection_ack")) acked.complete(null);
              if (m.contains("\"next\"") && ++nexts[0] == closeAfter) { w.sendClose(1000, "bye"); }
              if (m.contains("\"complete\"") || m.contains("\"type\":\"error\"")) done.complete(null);
            }
            w.request(1); return null;
          }
          public CompletionStage<?> onClose(WebSocket w, int code, String reason) {
            System.out.printf("%5d ms  closed %d %s%n", (System.nanoTime() - t0) / 1_000_000, code, reason);
            done.complete(null); return null;
          }
        }).get(5, TimeUnit.SECONDS);
    ws.sendText("{\"type\":\"connection_init\"}", true);
    acked.get(5, TimeUnit.SECONDS);
    String q = doc.replace("\\", "\\\\").replace("\"", "\\\"");
    ws.sendText("{\"id\":\"1\",\"type\":\"subscribe\",\"payload\":{\"query\":\"" + q + "\"}}", true);
    try { done.get(secs, TimeUnit.SECONDS); } catch (TimeoutException e) { System.out.println("(timeout)"); }
    ws.abort();
  }
}

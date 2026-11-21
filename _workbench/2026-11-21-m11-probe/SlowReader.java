// Subscribes over graphql-transport-ws and reads only <perSecond> frames a second (WebSocket.request(1) pacing).
//   java SlowReader.java <ws url> <document> <perSecond> <seconds>
import java.net.URI;
import java.net.http.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SlowReader {
  public static void main(String[] a) throws Exception {
    String url = a[0], doc = a[1]; int rate = Integer.parseInt(a[2]), secs = Integer.parseInt(a[3]);
    var got = new AtomicInteger(); var last = new String[1];
    var sched = Executors.newSingleThreadScheduledExecutor();
    var acked = new CompletableFuture<Void>();
    WebSocket ws = HttpClient.newHttpClient().newWebSocketBuilder().subprotocols("graphql-transport-ws")
        .buildAsync(URI.create(url), new WebSocket.Listener() {
          public void onOpen(WebSocket w) { w.request(1); }
          public CompletionStage<?> onText(WebSocket w, CharSequence d, boolean l) {
            String m = d.toString();
            if (m.contains("connection_ack")) { acked.complete(null); w.request(1); return null; }
            got.incrementAndGet(); last[0] = m;
            sched.schedule(() -> w.request(1), 1000 / rate, TimeUnit.MILLISECONDS);
            return null;
          }
          public CompletionStage<?> onClose(WebSocket w, int c, String r) { System.out.println("closed " + c + " " + r); return null; }
        }).get(5, TimeUnit.SECONDS);
    ws.sendText("{\"type\":\"connection_init\"}", true);
    acked.get(5, TimeUnit.SECONDS);
    ws.sendText("{\"id\":\"1\",\"type\":\"subscribe\",\"payload\":{\"query\":\"" + doc.replace("\"", "\\\"") + "\"}}", true);
    for (int s = 1; s <= secs; s++) { Thread.sleep(1000); System.out.println("t=" + s + "s received=" + got.get() + " last=" + last[0]); }
    ws.abort(); sched.shutdownNow();
  }
}

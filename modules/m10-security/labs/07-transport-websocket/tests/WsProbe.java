// A graphql-transport-ws client for lab 10.7's tests. JDK only.
//   java WsProbe.java <ws url> <origin> <token or -> <clubId> <trigger token>
// Opens the socket from <origin>, sends connection_init (with the token, if any), subscribes to
// reviewAdded(clubId), then adds a review to that club over HTTP as <trigger token> and waits 5 s.
// Prints exactly one outcome line:
//   handshake-refused <status> | closed <code> <reason> | error <message> | next <review body> | silent
import java.net.URI;
import java.net.http.*;
import java.util.concurrent.*;

public class WsProbe {
  public static void main(String[] a) throws Exception {
    String url = a[0], origin = a[1], token = a[2], club = a[3], trigger = a[4];
    var outcome = new CompletableFuture<String>();
    var acked = new CompletableFuture<Void>();
    var http = HttpClient.newHttpClient();
    WebSocket ws;
    try {
      ws = http.newWebSocketBuilder().header("Origin", origin).subprotocols("graphql-transport-ws")
          .buildAsync(URI.create(url), new WebSocket.Listener() {
            StringBuilder buf = new StringBuilder();
            public CompletionStage<?> onText(WebSocket w, CharSequence data, boolean last) {
              buf.append(data);
              if (last) {
                String m = buf.toString();
                buf.setLength(0);
                if (m.contains("\"connection_ack\"")) acked.complete(null);
                else if (m.contains("\"type\":\"next\"")) outcome.complete("next " + m.replaceAll(".*\"body\":\"([^\"]*)\".*", "$1"));
                else if (m.contains("\"type\":\"error\"")) outcome.complete("error " + m.replaceAll(".*\"message\":\"([^\"]*)\".*", "$1"));
              }
              w.request(1);
              return null;
            }
            public CompletionStage<?> onClose(WebSocket w, int code, String reason) {
              outcome.complete("closed " + code + " " + reason);
              acked.complete(null);
              return null;
            }
          }).get(5, TimeUnit.SECONDS);
    } catch (ExecutionException e) {
      String msg = String.valueOf(e.getCause());
      System.out.println("handshake-refused " + (e.getCause() instanceof WebSocketHandshakeException h ? h.getResponse().statusCode() : msg));
      return;
    }
    String payload = token.equals("-") ? "{}" : "{\"Authorization\":\"Bearer " + token + "\"}";
    ws.sendText("{\"type\":\"connection_init\",\"payload\":" + payload + "}", true);
    try { acked.get(5, TimeUnit.SECONDS); } catch (TimeoutException e) { System.out.println("silent"); return; }
    if (outcome.isDone()) { System.out.println(outcome.get()); return; }
    ws.sendText("{\"id\":\"1\",\"type\":\"subscribe\",\"payload\":{\"query\":\"subscription { reviewAdded(clubId: \\\"" + club
        + "\\\") { body } }\"}}", true);
    Thread.sleep(500);
    String http0 = url.replace("ws://", "http://");
    http.send(HttpRequest.newBuilder(URI.create(http0)).header("Content-Type", "application/json").header("Authorization", "Bearer " + trigger)
        .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"mutation { addReview(clubId: \\\"" + club
            + "\\\", bookId: \\\"b2\\\", rating: 3, body: \\\"live note\\\") { id } }\"}")).build(), HttpResponse.BodyHandlers.ofString());
    try { System.out.println(outcome.get(5, TimeUnit.SECONDS)); } catch (TimeoutException e) { System.out.println("silent"); }
    ws.abort();
  }
}

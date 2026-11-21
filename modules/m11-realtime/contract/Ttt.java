// Game 11.9's group variant: play tic-tac-toe (or watch) from a terminal.
//   bin/course java modules/m11-realtime/contract/Ttt.java <http url> <match id> <your name> [--watch]
// Joins the match (unless --watch), draws the board on every update and, when it is your move, reads a cell
// number (0-8) from the keyboard.
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.util.Map;
import java.util.concurrent.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class Ttt {
  static final JsonMapper json = JsonMapper.builder().build();
  static final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
  static String url, id, me, mark;

  public static void main(String[] a) throws Exception {
    url = a[0]; id = a[1]; me = a[2];
    boolean watch = a.length > 3 && a[3].equals("--watch");
    if (!watch) {
      JsonNode j = gql("mutation($id: ID!, $n: String!) { joinMatch(id: $id, name: $n) }", Map.of("id", id, "n", me));
      if (j.has("errors")) { System.out.println("could not join: " + j.path("errors").get(0).path("message").asString()); return; }
      mark = j.path("data").path("joinMatch").asString();
      System.out.println("you play " + mark);
    }
    var over = new CompletableFuture<Void>();
    var acked = new CompletableFuture<Void>();
    WebSocket ws = http.newWebSocketBuilder().subprotocols("graphql-transport-ws").buildAsync(URI.create(url.replaceFirst("^http", "ws")),
        new WebSocket.Listener() {
          final StringBuilder buf = new StringBuilder();
          public CompletionStage<?> onText(WebSocket w, CharSequence d, boolean last) {
            buf.append(d);
            if (last) {
              JsonNode f = json.readTree(buf.toString());
              buf.setLength(0);
              switch (f.path("type").asString()) {
                case "connection_ack" -> acked.complete(null);
                case "ping" -> w.sendText("{\"type\":\"pong\"}", true);
                case "next" -> draw(f.path("payload").path("data").path("matchUpdated"));
                case "complete", "error" -> over.complete(null);
                default -> {}
              }
            }
            w.request(1);
            return null;
          }
          public CompletionStage<?> onClose(WebSocket w, int c, String r) { over.complete(null); return null; }
        }).get(5, TimeUnit.SECONDS);
    ws.sendText("{\"type\":\"connection_init\"}", true);
    acked.get(5, TimeUnit.SECONDS);
    String sub = "subscription($id: ID!, $as: String) { matchUpdated(id: $id, as: $as) { board next winner draw players spectators } }";
    var vars = new java.util.HashMap<String, Object>(Map.of("id", id));
    if (!watch) vars.put("as", me);
    ws.sendText(json.writeValueAsString(Map.of("id", "1", "type", "subscribe", "payload", Map.of("query", sub, "variables", vars))), true);
    if (!watch) Thread.ofVirtual().start(Ttt::keyboard);
    over.get();
    System.exit(0);
  }

  static void draw(JsonNode m) {
    StringBuilder b = new StringBuilder("\n");
    for (int r = 0; r < 3; r++) {
      for (int c = 0; c < 3; c++) {
        String v = m.path("board").get(r * 3 + c).asString();
        b.append(' ').append(v.isEmpty() ? String.valueOf(r * 3 + c) : v).append(c < 2 ? " |" : "\n");
      }
      if (r < 2) b.append("---+---+---\n");
    }
    b.append("players: ").append(m.path("players")).append(", spectators: ").append(m.path("spectators").asInt()).append('\n');
    if (!m.path("winner").isNull()) b.append(m.path("winner").asString()).append(" wins\n");
    else if (m.path("draw").asBoolean()) b.append("draw\n");
    else if (!m.path("next").isNull()) b.append(m.path("next").asString().equals(mark) ? "your move> " : m.path("next").asString() + " to move\n");
    System.out.print(b);
  }

  static void keyboard() {
    try (var in = new BufferedReader(new InputStreamReader(System.in))) {
      for (String line; (line = in.readLine()) != null; ) {
        if (!line.trim().matches("\\d")) continue;
        JsonNode r = gql("mutation($id: ID!, $n: String!, $c: Int!) { move(id: $id, name: $n, cell: $c) { moves } }",
            Map.of("id", id, "n", me, "c", Integer.parseInt(line.trim())));
        if (r.has("errors")) System.out.println("refused: " + r.path("errors").get(0).path("extensions").path("code").asString());
      }
    } catch (Exception e) {
      System.out.println("keyboard: " + e);
    }
  }

  static JsonNode gql(String q, Map<String, Object> vars) throws Exception {
    var res = http.send(HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("query", q, "variables", vars)))).build(), HttpResponse.BodyHandlers.ofString());
    return json.readTree(res.body());
  }
}

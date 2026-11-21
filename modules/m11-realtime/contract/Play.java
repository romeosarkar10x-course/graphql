// Mini-project 11.8's group variant: play a trivia game from a terminal.
//   bin/course java modules/m11-realtime/contract/Play.java <http url> <game id> <your name>
// Joins the game, prints each question as it is asked, reads your choice (0-3) from the keyboard and sends
// it, and prints the leaderboard after each question. The host creates and starts the game with curl.
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.util.Map;
import java.util.concurrent.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class Play {
  static final JsonMapper json = JsonMapper.builder().build();
  static final HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
  static String url, game, me;

  public static void main(String[] a) throws Exception {
    url = a[0]; game = a[1]; me = a[2];
    JsonNode joined = gql("mutation($g: ID!, $n: String!) { joinGame(gameId: $g, name: $n) { name } }", Map.of("g", game, "n", me));
    if (joined.has("errors")) { System.out.println("could not join: " + joined.path("errors").get(0).path("message").asString()); return; }
    System.out.println("joined " + game + " as " + me + "; waiting for the host to start");
    var over = new CompletableFuture<Void>();
    var acked = new CompletableFuture<Void>();
    WebSocket ws = http.newWebSocketBuilder().subprotocols("graphql-transport-ws").buildAsync(URI.create(url.replaceFirst("^http", "ws")),
        new WebSocket.Listener() {
          final StringBuilder buf = new StringBuilder();
          public CompletionStage<?> onText(WebSocket w, CharSequence d, boolean last) {
            buf.append(d);
            if (last) { handle(w, json.readTree(buf.toString()), acked, over); buf.setLength(0); }
            w.request(1);
            return null;
          }
          public CompletionStage<?> onClose(WebSocket w, int code, String reason) { over.complete(null); return null; }
        }).get(5, TimeUnit.SECONDS);
    ws.sendText("{\"type\":\"connection_init\"}", true);
    acked.get(5, TimeUnit.SECONDS);
    String sub = "subscription($g: ID!) { gameEvents(gameId: $g) { __typename ... on QuestionAsked { question { index text choices } secondsToAnswer }"
        + " ... on QuestionClosed { index correct answers } ... on LeaderboardChanged { leaderboard { name score } } ... on GameOver { leaderboard { name score } } } }";
    ws.sendText(json.writeValueAsString(Map.of("id", "1", "type", "subscribe", "payload", Map.of("query", sub, "variables", Map.of("g", game)))), true);
    Thread.ofVirtual().start(Play::keyboard);
    over.get();
    System.exit(0);
  }

  static void handle(WebSocket w, JsonNode f, CompletableFuture<Void> acked, CompletableFuture<Void> over) {
    switch (f.path("type").asString()) {
      case "connection_ack" -> acked.complete(null);
      case "ping" -> w.sendText("{\"type\":\"pong\"}", true);
      case "complete", "error" -> over.complete(null);
      case "next" -> {
        JsonNode e = f.path("payload").path("data").path("gameEvents");
        switch (e.path("__typename").asString()) {
          case "QuestionAsked" -> {
            JsonNode q = e.path("question");
            System.out.printf("%nQuestion %d (%d s): %s%n", q.path("index").asInt() + 1, e.path("secondsToAnswer").asInt(), q.path("text").asString());
            for (int i = 0; i < q.path("choices").size(); i++) System.out.printf("  %d  %s%n", i, q.path("choices").get(i).asString());
            System.out.print("your answer> ");
          }
          case "QuestionClosed" -> System.out.printf("%nclosed: the answer was %d (%d answered)%n", e.path("correct").asInt(), e.path("answers").asInt());
          case "LeaderboardChanged", "GameOver" -> {
            System.out.println(e.path("__typename").asString().equals("GameOver") ? "GAME OVER" : "leaderboard:");
            for (JsonNode p : e.path("leaderboard")) System.out.printf("  %-12s %5d%s%n", p.path("name").asString(), p.path("score").asInt(), p.path("name").asString().equals(me) ? "  <- you" : "");
          }
          default -> {}
        }
      }
      default -> {}
    }
  }

  static void keyboard() {
    try (var in = new BufferedReader(new InputStreamReader(System.in))) {
      for (String line; (line = in.readLine()) != null; ) {
        if (!line.trim().matches("\\d+")) continue;
        JsonNode r = gql("mutation($g: ID!, $n: String!, $c: Int!) { answer(gameId: $g, name: $n, choice: $c) { accepted reason } }",
            Map.of("g", game, "n", me, "c", Integer.parseInt(line.trim())));
        JsonNode ans = r.path("data").path("answer");
        System.out.println(ans.path("accepted").asBoolean() ? "sent" : "not accepted: " + ans.path("reason").asString());
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

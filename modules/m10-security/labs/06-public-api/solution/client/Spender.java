// A well-behaved API client (lab 10.6): it spends its budget as fast as the server restores it, and no faster.
//   bin/course java client/Spender.java <url> <api key> <requests>
import java.net.URI;
import java.net.http.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class Spender {
  static final String QUERY = "{ books(first: 20) { id title reviews(first: 2) { rating } } }";

  public static void main(String[] args) throws Exception {
    var http = HttpClient.newHttpClient();
    var json = JsonMapper.builder().build();
    String body = json.writeValueAsString(java.util.Map.of("query", QUERY));
    long available = Long.MAX_VALUE, restore = 1, lastCost = 0;   // unknown until the first response
    for (int i = 1, retries = 0; i <= Integer.parseInt(args[2]); ) {
      // Wait for the bucket to hold what the next request will cost (the last cost is the best guess).
      if (available < lastCost) Thread.sleep((lastCost - available) * 1000 / restore + 1);
      var res = http.send(HttpRequest.newBuilder(URI.create(args[0])).header("Content-Type", "application/json")
          .header("X-Api-Key", args[1]).POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
      JsonNode r = json.readTree(res.body());
      JsonNode cost = r.at("/extensions/cost");
      if (cost.isMissingNode()) cost = r.at("/errors/0/extensions/cost");
      if (cost.isMissingNode()) throw new IllegalStateException("no cost information: " + res.body());
      lastCost = cost.get("requestedQueryCost").asLong();
      available = cost.at("/throttleStatus/currentlyAvailable").asLong();
      restore = cost.at("/throttleStatus/restoreRate").asLong();
      if (r.has("data") && !r.get("data").isNull()) {
        System.out.printf("request %d: cost %d, %d left%n", i++, lastCost, available);
        retries = 0;
      } else if (++retries > 5) {
        throw new IllegalStateException("throttled 5 times in a row: " + res.body());
      } else {
        System.out.printf("throttled: %d available, waiting%n", available);
      }
    }
  }
}

// Cost meter (Module 10 mini-project): a proxy in front of a GraphQL API that reads each response's
// extensions.cost and shows every client's spend per minute.
//   bin/course java CostMeter.java <listen port> <upstream url>
//   GET  /        the dashboard (HTML, refreshes itself every 2 s)
//   GET  /meter   the numbers: {"clients": [{"key", "minutes": [{"minute", "cost", "requests", "throttled"}]}]}
//   POST /graphql forwarded to <upstream url> unchanged, response returned unchanged
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class CostMeter {
  static final JsonMapper JSON = JsonMapper.builder().build();
  static final HttpClient HTTP = HttpClient.newHttpClient();
  static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm").withZone(ZoneOffset.UTC);
  // key -> minute -> [cost, requests, throttled]
  static final Map<String, SortedMap<String, long[]>> SPEND = new ConcurrentHashMap<>();

  public static void main(String[] a) throws Exception {
    int port = Integer.parseInt(a[0]);
    URI upstream = URI.create(a[1]);
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    server.createContext("/graphql", ex -> proxy(ex, upstream));
    server.createContext("/meter", ex -> send(ex, 200, "application/json", meter()));
    server.createContext("/", ex -> send(ex, 200, "text/html; charset=utf-8", PAGE));
    server.start();
    System.out.println("cost meter on :" + port + " -> " + upstream);
  }

  static void proxy(HttpExchange ex, URI upstream) throws IOException {
    byte[] body = ex.getRequestBody().readAllBytes();
    var req = HttpRequest.newBuilder(upstream).method(ex.getRequestMethod(), HttpRequest.BodyPublishers.ofByteArray(body));
    for (String h : List.of("Content-Type", "Accept", "Authorization", "X-Api-Key")) {
      String v = ex.getRequestHeaders().getFirst(h);
      if (v != null) req.header(h, v);
    }
    HttpResponse<byte[]> res;
    try {
      res = HTTP.send(req.build(), HttpResponse.BodyHandlers.ofByteArray());
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
    record(ex.getRequestHeaders().getFirst("X-Api-Key"), res.body());
    ex.getResponseHeaders().set("Content-Type", res.headers().firstValue("Content-Type").orElse("application/json"));
    ex.sendResponseHeaders(res.statusCode(), res.body().length == 0 ? -1 : res.body().length);
    try (OutputStream out = ex.getResponseBody()) { out.write(res.body()); }
  }

  /** A served request spends its requestedQueryCost; a throttled one spends nothing and is counted apart. */
  static void record(String key, byte[] body) {
    if (key == null) key = "(no key)";
    JsonNode r;
    try { r = JSON.readTree(body); } catch (Exception e) { return; }
    boolean throttled = "THROTTLED".equals(r.at("/errors/0/extensions/code").asString(""));
    long cost = throttled ? 0 : r.at("/extensions/cost/requestedQueryCost").asLong(0);
    String minute = MINUTE.format(Instant.now());
    long[] row = SPEND.computeIfAbsent(key, k -> new ConcurrentSkipListMap<>()).computeIfAbsent(minute, m -> new long[3]);
    synchronized (row) {
      row[0] += cost;
      row[1]++;
      if (throttled) row[2]++;
    }
  }

  static String meter() {
    List<Map<String, Object>> clients = new ArrayList<>();
    for (var e : new TreeMap<>(SPEND).entrySet()) {
      List<Map<String, Object>> minutes = new ArrayList<>();
      for (var m : e.getValue().entrySet()) {
        long[] v = m.getValue();
        synchronized (v) { minutes.add(Map.of("minute", m.getKey(), "cost", v[0], "requests", v[1], "throttled", v[2])); }
      }
      clients.add(Map.of("key", e.getKey(), "minutes", minutes));
    }
    return JSON.writeValueAsString(Map.of("clients", clients));
  }

  static void send(HttpExchange ex, int status, String type, String body) throws IOException {
    byte[] b = body.getBytes(StandardCharsets.UTF_8);
    ex.getResponseHeaders().set("Content-Type", type);
    ex.sendResponseHeaders(status, b.length);
    try (OutputStream out = ex.getResponseBody()) { out.write(b); }
  }

  static final String PAGE = """
      <!doctype html><html><head><meta charset="utf-8"><title>Shelfie cost meter</title>
      <style>body{font:14px system-ui;margin:2rem;background:#fff;color:#222}table{border-collapse:collapse}
      td,th{padding:.3rem .8rem;border-bottom:1px solid #ddd;text-align:right}th:first-child,td:first-child{text-align:left}
      .bar{background:#4a7;height:.8rem;display:inline-block}.t{color:#b33}</style></head>
      <body><h1>Cost spent per client, per minute</h1><table id="t"></table><p>Refreshes every 2 s from <code>/meter</code>.</p>
      <script>
      async function draw(){const d=await (await fetch('/meter')).json();let max=1;
        d.clients.forEach(c=>c.minutes.forEach(m=>max=Math.max(max,m.cost)));
        let h='<tr><th>Client</th><th>Minute (UTC)</th><th>Cost</th><th>Requests</th><th>Throttled</th><th></th></tr>';
        d.clients.forEach(c=>c.minutes.slice(-5).forEach(m=>{h+=`<tr><td>${c.key}</td><td>${m.minute}</td><td>${m.cost}</td>`+
          `<td>${m.requests}</td><td class="t">${m.throttled||''}</td><td><span class="bar" style="width:${200*m.cost/max}px"></span></td></tr>`}));
        document.getElementById('t').innerHTML=h;}
      draw();setInterval(draw,2000);
      </script></body></html>
      """;
}

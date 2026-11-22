// A TCP load balancer that sends new connections only to ready backends, as a Kubernetes Service does with
// readiness probes. Module 16.4's rolling deploy. JDK only; never looks inside the bytes (HTTP and WebSocket alike).
//   java ReadyBalancer.java <listen port> <backend port>[,<backend port>...] [--propagation-ms 1000]
// Every 100 ms it asks each backend GET /actuator/health/readiness: 200 is ready, anything else (503, refused,
// timeout, 404) is not. A change takes effect --propagation-ms later, as endpoint updates take a moment to reach
// every node in a cluster. New connections go round-robin to backends it believes ready; a backend that refuses is
// NOT retried (the client sees its connection close, as with kube-proxy). Open connections are never moved.
// It prints one line per change, with milliseconds since start:  "  1234 4001 ready" / "  5678 4001 not-ready"
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ReadyBalancer {
  static final Map<Integer, Boolean> ready = new ConcurrentHashMap<>();
  static final Map<Integer, Boolean> seen = new ConcurrentHashMap<>();
  static long propagationMs = 1000;
  static final ScheduledExecutorService later = Executors.newSingleThreadScheduledExecutor();
  static final long t0 = System.nanoTime();

  public static void main(String[] a) throws Exception {
    int listen = Integer.parseInt(a[0]);
    int[] backends = Arrays.stream(a[1].split(",")).mapToInt(Integer::parseInt).toArray();
    if (a.length > 3 && a[2].equals("--propagation-ms")) propagationMs = Long.parseLong(a[3]);
    for (int b : backends) { ready.put(b, false); seen.put(b, false); }
    var http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(200)).build();
    for (int b : backends) Thread.ofVirtual().start(() -> probe(http, b));
    var next = new AtomicInteger();
    try (ServerSocket server = new ServerSocket()) {
      server.setReuseAddress(true);
      server.bind(new InetSocketAddress(listen));
      log("balancing :" + listen + " over " + a[1]);
      while (true) {
        Socket client = server.accept();
        Thread.ofVirtual().start(() -> connect(client, backends, next));
      }
    }
  }

  static void probe(HttpClient http, int port) {
    var req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health/readiness"))
        .timeout(Duration.ofMillis(300)).build();
    while (true) {
      boolean ok;
      try {
        ok = http.send(req, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
      } catch (Exception e) {
        ok = false;
      }
      final boolean now = ok;
      if (seen.put(port, ok) != ok) later.schedule(() -> { ready.put(port, now); log(port + (now ? " ready" : " not-ready")); },
          propagationMs, TimeUnit.MILLISECONDS);
      try { Thread.sleep(100); } catch (InterruptedException e) { return; }
    }
  }

  static void connect(Socket client, int[] backends, AtomicInteger next) {
    for (int tries = 0; tries < backends.length; tries++) {
      int port = backends[Math.floorMod(next.getAndIncrement(), backends.length)];
      if (!ready.get(port)) continue;
      try {
        Socket up = new Socket("localhost", port);
        Thread.ofVirtual().start(() -> pipe(client, up));
        Thread.ofVirtual().start(() -> pipe(up, client));
      } catch (IOException refused) {
        close(client);   // no retry on another backend
      }
      return;
    }
    close(client);   // no ready backend: the client sees a closed connection
  }

  static void pipe(Socket from, Socket to) {
    try (InputStream in = from.getInputStream(); OutputStream out = to.getOutputStream()) {
      in.transferTo(out);
    } catch (IOException e) {
      // a closed side ends both
    } finally {
      close(from);
      close(to);
    }
  }

  static void close(Socket s) { try { s.close(); } catch (IOException ignored) {} }

  static synchronized void log(String s) {
    System.out.printf("%6d %s%n", (System.nanoTime() - t0) / 1_000_000, s);
    System.out.flush();
  }
}

// A round-robin TCP load balancer with a "network drop" switch, for Module 11's scaling and reconnect labs.
// JDK only. Works for HTTP, SSE and WebSocket alike, because it never looks inside the bytes.
//   java Balancer.java <listen port> <backend port>[,<backend port>...] [--control <file>]
// Each new connection goes to the next backend (localhost) in turn; a backend that refuses is skipped.
// Drop switch: write "drop <ms>" to the control file (default /tmp/balancer-<port>) to close every open
// connection and refuse new ones for <ms> milliseconds, as a network blip would.
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Balancer {
  static final Set<Socket> open = ConcurrentHashMap.newKeySet();
  static volatile long refuseUntil = 0;

  public static void main(String[] a) throws Exception {
    int listen = Integer.parseInt(a[0]);
    int[] backends = Arrays.stream(a[1].split(",")).mapToInt(Integer::parseInt).toArray();
    Path control = Path.of(a.length > 3 && a[2].equals("--control") ? a[3] : "/tmp/balancer-" + listen);
    Files.deleteIfExists(control);
    var next = new AtomicInteger();
    Thread.ofVirtual().start(() -> watch(control));
    try (ServerSocket server = new ServerSocket()) {
      server.setReuseAddress(true);
      server.bind(new InetSocketAddress(listen));
      System.out.println("balancing :" + listen + " over " + a[1] + " (control file " + control + ")");
      while (true) {
        Socket client = server.accept();
        if (System.currentTimeMillis() < refuseUntil) { client.setSoLinger(true, 0); client.close(); continue; }
        Thread.ofVirtual().start(() -> connect(client, backends, next));
      }
    }
  }

  static void connect(Socket client, int[] backends, AtomicInteger next) {
    for (int tries = 0; tries < backends.length; tries++) {
      int port = backends[Math.floorMod(next.getAndIncrement(), backends.length)];
      try {
        Socket up = new Socket("localhost", port);
        open.add(client);
        open.add(up);
        Thread.ofVirtual().start(() -> pipe(client, up));
        Thread.ofVirtual().start(() -> pipe(up, client));
        return;
      } catch (IOException refused) {
        // try the next backend
      }
    }
    close(client);
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

  static void close(Socket s) {
    open.remove(s);
    try { s.close(); } catch (IOException ignored) {}
  }

  static void watch(Path control) {
    while (true) {
      try {
        Thread.sleep(50);
        if (!Files.exists(control)) continue;
        String cmd = Files.readString(control).trim();
        Files.delete(control);
        if (cmd.startsWith("drop")) {
          long ms = cmd.split("\\s+").length > 1 ? Long.parseLong(cmd.split("\\s+")[1]) : 0;
          refuseUntil = System.currentTimeMillis() + ms;
          for (Socket s : List.copyOf(open)) {
            try { s.setSoLinger(true, 0); } catch (IOException ignored) {}
            close(s);
          }
          System.out.println("dropped every connection; refusing for " + ms + " ms");
        }
      } catch (Exception e) {
        // keep watching
      }
    }
  }
}

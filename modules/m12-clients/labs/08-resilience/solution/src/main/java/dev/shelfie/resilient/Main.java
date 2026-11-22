package dev.shelfie.resilient;

import dev.shelfie.resilient.Shelfie.MyReview;
import dev.shelfie.resilient.Shelfie.ReviewPayload;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.client.ResourceAccessException;

/**
 * Lab 12.8.
 *   java -jar target/shelfie-resilient.jar <url> load <n> [--plain]
 *   java -jar target/shelfie-resilient.jar <url> reviews <user> <bookId>...
 */
public class Main {
  public static void main(String[] args) throws Exception {
    String url = args[0];
    switch (args[1]) {
      case "load" -> load(url, Integer.parseInt(args[2]), Arrays.asList(args).contains("--plain"));
      case "reviews" -> reviews(url, args[2], Arrays.asList(args).subList(3, args.length));
      default -> throw new IllegalArgumentException("load or reviews");
    }
  }

  /** n book queries; prints "ok <k>/<n>, failed <f>, circuit open <c>, <ms> ms". */
  static void load(String url, int n, boolean plain) {
    Shelfie shelfie = new Shelfie(url, "u1");
    Resilient resilient = new Resilient();
    int ok = 0, failed = 0, open = 0;
    long t0 = System.nanoTime();
    for (int i = 0; i < n; i++) {
      String id = "b" + (i % 10 + 1);
      try {
        Shelfie.Book b = plain ? shelfie.book(id) : resilient.call(() -> shelfie.book(id));
        if (b != null && b.id().equals(id)) ok++; else failed++;
      } catch (CallNotPermittedException e) {
        open++;
      } catch (RuntimeException e) {
        failed++;
      }
    }
    System.out.printf("ok %d/%d, failed %d, circuit open %d, %d ms%n", ok, n, failed, open, (System.nanoTime() - t0) / 1_000_000);
  }

  /**
   * createReview is not idempotent, so it is never retried blindly. A transport failure means "unknown": the
   * server may have stored the review and lost only the answer. So retry, and if the retry says DUPLICATE
   * after an unknown outcome, the earlier attempt worked: look the review up instead of reporting an error.
   * Prints "<bookId> <reviewId> created|recovered" or "<bookId> error <code or message>".
   */
  static void reviews(String url, String user, List<String> books) throws InterruptedException {
    Shelfie shelfie = new Shelfie(url, user);
    Resilient resilient = new Resilient();
    for (String bookId : books) {
      boolean unknown = false;
      String line = bookId + " error gave up";
      for (int attempt = 0; attempt < 8; attempt++) {
        try {
          ReviewPayload p = resilient.once(() -> shelfie.createReview(bookId, 4, "Written through a flaky network."));
          if (p.userErrors().isEmpty()) { line = bookId + " " + p.review().id() + " created"; break; }
          String code = p.userErrors().getFirst().code();
          if (code.equals("DUPLICATE") && unknown) {
            List<MyReview> mine = resilient.call(() -> shelfie.reviewsOf(user));
            String id = mine.stream().filter(r -> r.book().id().equals(bookId)).map(MyReview::id).findFirst().orElse("?");
            line = bookId + " " + id + " recovered";
          } else {
            line = bookId + " error " + code;
          }
          break;
        } catch (ResourceAccessException e) {
          unknown = true;
          Thread.sleep(resilient.backoffMillis(attempt));
        } catch (CallNotPermittedException e) {
          line = bookId + " error circuit open";
          break;
        }
      }
      System.out.println(line);
    }
  }
}

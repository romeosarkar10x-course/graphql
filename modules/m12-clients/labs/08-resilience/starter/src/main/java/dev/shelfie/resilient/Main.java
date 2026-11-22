package dev.shelfie.resilient;

import dev.shelfie.resilient.Shelfie.ReviewPayload;
import java.util.Arrays;
import java.util.List;

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

  /** n book queries; prints "ok <k>/<n>, failed <f>, circuit open <c>, <ms> ms". --plain: one attempt each, no Resilient. */
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
      } catch (RuntimeException e) {
        failed++; // TODO: count calls the circuit breaker refused as "circuit open"
      }
    }
    System.out.printf("ok %d/%d, failed %d, circuit open %d, %d ms%n", ok, n, failed, open, (System.nanoTime() - t0) / 1_000_000);
  }

  /**
   * One createReview per book, as user. Prints "<bookId> <reviewId> created|recovered" or "<bookId> error <reason>".
   * TODO: a lost response is not a failed mutation.
   */
  static void reviews(String url, String user, List<String> books) {
    Shelfie shelfie = new Shelfie(url, user);
    Resilient resilient = new Resilient();
    for (String bookId : books) {
      try {
        ReviewPayload p = resilient.once(() -> shelfie.createReview(bookId, 4, "Written through a flaky network."));
        System.out.println(p.userErrors().isEmpty() ? bookId + " " + p.review().id() + " created" : bookId + " error " + p.userErrors().getFirst().code());
      } catch (RuntimeException e) {
        System.out.println(bookId + " error " + e.getClass().getSimpleName());
      }
    }
  }
}

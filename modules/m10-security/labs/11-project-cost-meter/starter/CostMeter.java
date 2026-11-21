// Cost meter (Module 10 mini-project): a proxy in front of a GraphQL API that reads each response's
// extensions.cost and shows every client's spend per minute.
//   bin/course java CostMeter.java <listen port> <upstream url>
//   GET  /        the dashboard (HTML)
//   GET  /meter   {"clients": [{"key": "pk_...", "minutes": [{"minute": "2026-11-21T16:40", "cost": 0, "requests": 0, "throttled": 0}]}]}
//   POST /graphql forwarded to <upstream url> unchanged (keep Content-Type, Authorization, X-Api-Key), response unchanged
// A served request spends its extensions.cost.requestedQueryCost; a THROTTLED one spends nothing and counts as throttled.
// The JDK has an HTTP server (com.sun.net.httpserver) and client; Jackson 3 is on bin/course java's class path.
public class CostMeter {
  public static void main(String[] args) {
    throw new UnsupportedOperationException("TODO mini-project 10");
  }
}

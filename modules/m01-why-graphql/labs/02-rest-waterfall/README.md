# Lab 1.2 — The REST waterfall

**Lecture:** [1.2 The pain it targets](../../lectures/1.2-the-pain.md) · **Grading:** auto + rubric · **Needs:** `bin/course ref up`

Build Shelfie's club page from the reference server's REST API, then add 700 ms of latency per round
trip (the p99 PayPal measured in Checkout) and watch what happens.

## Do

1. `bin/course start m01/02`, read [the contract](../../contract/club-page.md), and finish `RestClubPage.java`.
   Run it from the lab folder: `../../../bin/course java RestClubPage.java c1`.
2. Get it correct first (sequential `get` calls are fine). The test checks c1, c2 and c3 against
   `contract/expected/` and checks your `requests=` against the server's own count.
3. Then make it fast: requests that do not depend on each other can go out together
   (`HTTP.sendAsync` returns a `CompletableFuture`).
4. Experiment, from the lab folder:
   ```bash
   ../../../bin/course toxi latency 700
   SHELFIE_URL=http://toxiproxy:8666 ../../../bin/course java RestClubPage.java c1 >/dev/null
   ../../../bin/course toxi reset
   ```
5. Fill in `notes.md`.

## Done when

`bin/course test m01/02` is green and `bin/course grade m01/02` gives 7/10+.

# Lab 0.4 — One request under the hood

**Lecture:** [0.4 One request under the hood](../../lectures/0.4-under-the-hood.md) · **Grading:** auto + rubric · **Needs:** `bin/course ref up`

No GraphQL library: just the JDK's `HttpClient` and Jackson, from jshell.

## Do

1. Finish `first-query.jsh` so it prints the title of the book that club `c1` is reading, and nothing else.
   Pass `c1` as a **variable**. Run it with `bin/course jshell first-query.jsh` from the lab folder.
2. Send `{ hello }` to the reference server three wrong ways and record each status code in
   `status-codes.txt`: as a GET, as a POST with `Content-Type: text/plain`, and as a POST with no body.
3. Answer `notes.md`.

The test counts requests on the server: exactly one GraphQL request is allowed.

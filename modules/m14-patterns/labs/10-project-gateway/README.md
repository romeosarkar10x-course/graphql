# Mini-project 14.10: Open Library gateway

Lecture: [14.4 GraphQL over existing systems](../../lectures/14.4-existing-systems.md). Grading: auto + rubric
(DESIGN.md). Needs nothing (the test runs a fake Open Library; `gateway.openlibrary-url` defaults to the real one).

One GraphQL API over someone else's REST API, `gateway.graphqls` in the starter: `book(isbn:)`, `books(isbns:)`,
`search(text:, first:)`. The starter answers correctly with one HTTP call per field and no defences. Make it a good
citizen and a reliable dependency:

- **One page, one call**: every edition a request needs comes from one `/api/books?bibkeys=...` call, each ISBN
  once (aliases, lists and repeats included).
- **Caching** for `gateway.ttl`, "no such ISBN" included; search by normalised text.
- **Stale answers**: while Open Library fails, an answer up to `gateway.stale-for` old is served with
  `stale: true` and no error; an ISBN never fetched is null with `UPSTREAM_UNAVAILABLE`, the page goes on.
- **Back-off**: timeouts (the page within 1.5 s), no calls for `gateway.cooldown` after a failure, and none before a
  429's `Retry-After` has passed. A `User-Agent` that names the gateway.

```bash
bin/course start m14/10
cd workspace/m14-patterns/10-project-gateway
bin/course shell -c 'mvn -q package && java -jar target/shelfie-gateway.jar'    # against the real Open Library
bin/course test m14/10                                                          # against the fake, with ttl 5 s
```

The test counts every call the fake receives ([contract/openlibrary](../../contract/openlibrary/)).

`DESIGN.md`: one page one call, freshness and failure, being a good client (and what yours does not do).

## Done when

`bin/course test m14/10` passes and `DESIGN.md` passes the rubric (`bin/course grade m14/10`).

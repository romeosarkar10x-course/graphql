# Lab 9.6 — Automatic persisted queries over GET, behind Varnish

**Lecture:** [9.6 Persisted queries for speed](../../lectures/9.6-persisted-queries.md) · **Grading:** both · **Needs:** postgres, varnish

Clients send the same few documents millions of times. Automatic persisted queries (APQ) let them send
a SHA-256 hash instead, and GET lets an HTTP cache keep the answer. The starter is the batched app;
Spring for GraphQL accepts POST only.

## Do

1. Accept GET: `spring.graphql.http.methods: GET,POST`. A mutation over GET must still be refused (405).
2. APQ with graphql-java's `ApolloPersistedQuerySupport` and your own `PersistedQueryCache` on Caffeine,
   installed with a `GraphQlSourceBuilderCustomizer` (`configureGraphQl(b -> b.preparsedDocumentProvider(...))`).
   The protocol: a hash the server doesn't know gets `PersistedQueryNotFound`; the client then sends the
   text and the hash, and the server checks that the hash matches the text before storing it
   (`PersistedQueryIdInvalid` if not). graphql-java checks the hash only on a cache *miss*: make a known
   hash sent with different text fail too.
3. `Cache-Control` on GET: `public, max-age=60` for anonymous responses without errors, `private, no-store`
   for anything with `X-Shelfie-User` or with errors (a `PersistedQueryNotFound` must never be cached),
   and `Vary: X-Shelfie-User`.

The four query texts and their hashes used by `tests/apq.hurl`:

```
c2b9944cecec09516421c4e2ccaa971b4d6d8cdfac70dfeaf8ce6c19e53bf06f  { club(id: 1) { name owner { name } } }
c9b6aa2e07262ba2c49b5020c907fef85dd6b18afc73be47f5bef1f630592632  { club(id: 3) { name memberCount } }
d6649267477e2a7f309461a0ef8e406d16897f5cb1e86bfa09dbe1a86d8708cf  { club(id: 4) { name } }
```

## Experiment

`Wire.java` sends the dashboard 50 times through Varnish three ways (POST, GET with the text, APQ GET)
and prints request bytes, hit rate and median latency. Run it with the app and Varnish up:
`bin/course shell -c '(java -jar target/shelfie-perf.jar > /tmp/app.log &) && sleep 15 && python3 /course/tools/varnish.py 4000 && java Wire.java'`.
Fill `notes.md`: what the numbers say, why APQ is not a security feature, and the traps you hit.

## Done when

`bin/course test m09/06` is green (10 protocol checks, fewer bytes than POST, a hit rate of 90% or more)
and `bin/course grade m09/06` passes the notes.

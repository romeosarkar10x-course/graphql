# Lab 9.5 — Four caching layers, five stale-data bugs

**Lecture:** [9.5 Caching layers](../../lectures/9.5-caching-layers.md) · **Grading:** both · **Needs:** postgres, redis, varnish (`bin/course ref up`)

A previous developer made the club app fast with four caches, and the bug reports started:

| Layer | Where | In the starter |
|---|---|---|
| request | the per-request DataLoaders (`books` by id, ...) | `BatchController` |
| entity | books by id, shared by all requests, in this JVM (Caffeine) | `BookCache` |
| response | whole query responses in Redis, shared by every instance; header `X-Response-Cache` | `ResponseCache` |
| http | `Cache-Control` on GET responses, for browsers and the CDN (Varnish, header `X-Cache`) | `HttpCaching` |

Each of `scenarios/scenario-1.hurl` to `scenario-5.hurl` reproduces one bug report, and each fails on
the starter. Each scenario needs a fresh start: seed data, an empty Redis, a new JVM, and Varnish pointed
at the app. The test does all of that per scenario. By hand, in one shell:

```
bin/course clubs m09_05
bin/course shell -c 'mvn -q package -DskipTests && (java -jar target/shelfie-perf.jar > /tmp/app.log &) && sleep 15 \
  && python3 /course/tools/varnish.py 4000 \
  && hurl --test --variable url=http://localhost:4000/graphql --variable varnish=http://varnish:6081/graphql scenarios/scenario-1.hurl'
```

`varnish.py` points the course Varnish at the container it runs in, so the app and the scenario must
share that shell.

## Do

1. For each scenario, find the guilty layer from the evidence: the response headers (`X-Response-Cache`,
   `X-Cache`, `Cache-Control`, `Age`), `extensions.sql.statements` (0 means nobody asked the database),
   and whether it happens inside one request or across requests. Write `N: layer` in `diagnosis.md`.
2. Fix it in that layer, without switching any cache off. Some symptoms have two causes in two layers:
   fixing one is not always enough, and that is part of the diagnosis.
3. The caches must still cache: the test checks that a repeated dashboard is a response-cache hit with
   no SQL at all.

## Done when

`bin/course test m09/05` passes all five scenarios and the diagnosis, and `bin/course grade m09/05`
passes the explanations in `diagnosis.md`.

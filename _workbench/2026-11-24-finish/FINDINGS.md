# Finishing the course: the open checks (2026-11-24)

The user, after the capstone: "you do the unfinished checks, accepting licenses, etc; No need to do open the html
pages in the browser to do visual testing. you can do everything else." Everything below was run on one Arch Linux
machine (24 cores), inside the course image unless it says otherwise.

## Gradle builds that were not allowed before

1. **graphql-java v26.1 (lab 17.7)**: `./gradlew test --tests 'graphql.parser.*'`: 219 tests, BUILD SUCCESSFUL in
   1 min 4 s the first time (`gj-parser-tests.txt`). The CRLF fix (`split("\\r\\n|[\\n\\r]", -1)`) plus a test:
   `StringValueParsingTest` 16 tests green. `RELEASE_VERSION=26.1-local ./gradlew publishToMavenLocal
   -PpublishToMavenLocal`: both flags work as read from `build.gradle`, 12 s (`gj-publish.txt`); inside the image
   `-Dmaven.repo.local=/course/.cache/m2` is needed. A pom on 26.1-local parses `"""\r\n    one\r\n    two\r\n"""`
   to `one\ntwo`; stock 26.1 gives `\r\none\r\ntwo\r` (`use-local.txt`). The clone was deleted afterwards.
2. **Viaduct cli-starter (lab 18.4)** at `d111e57` (release/v3.0.0), Gradle 9.1.0 on Java 25:
   `{"data": {"greeting": "Hello, World!"}}`, 1 min 4 s first run, 4 s after (`viaduct-run1.txt`, `viaduct-run2.txt`).

## Licences and telemetry

3. **Rover 1.0.1, ELv2 accepted** (`APOLLO_ELV2_LICENSE=accept`, the user's decision). SHA-256 of the Linux tarball
   matches GitHub's digest. Every course `supergraph.yaml` composed with Rover and with The Guild's composer
   (`rover/parity.sh`): identical up to directive order, Rover's extra `@join__directive`/`join__DirectiveArguments`
   definitions, and which subgraph's description wins for a shared type; lab 15.3's broken starter fails in both
   with `TYPE_KIND_MISMATCH`.
4. **Rover and `@oneOf`**: composition 2.9.0 and 2.15.6 reject an undeclared `@oneOf` ("Unknown directive"); with
   `directive @oneOf on INPUT_OBJECT` declared it composes but drops `@oneOf` from the supergraph, where The
   Guild's composer keeps it. graphql-java accepts the explicit declaration (`isOneOf=true`).
5. **Apollo Router telemetry** off (`APOLLO_TELEMETRY_DISABLED=true` in `fed.sh` and the 15.8 compose files);
   checkpoint 15's Docker Compose stack still comes up.

## Corrections to earlier findings

6. **Module 10 finding 4 was wrong on Boot 4.2.0**: with Keycloak stopped, lab 10.2's solution answered 500
   (`JwtDecoderInitializationException ... Unable to resolve the Configuration with the provided Issuer`). Boot's
   `JwtDecoderConfiguration` has three `@Bean` methods (public key, JWK set URI, issuer URI) and the issuer one is
   chosen when `issuer-uri` is set. Fix in 16 apps (labs 10.2 to 10.8 and 14.3, 14.6, solution and starter): no
   `issuer-uri`, a `JwtIssuerValidator` bean (Boot adds `OAuth2TokenValidator<Jwt>` beans to the decoder it builds
   from the key). Offline: 200 for a good token; 401 for a wrong issuer, expired, wrong audience; 0 Keycloak log
   lines. Labs 10.2, 10.4 to 10.8, 14.3, 14.6 pass with Keycloak stopped (`m10-offline.txt`).
7. **Module 13 finding 7, cause found**: `@AutoConfigureHttpGraphQlTester` alone wraps a `WebTestClient` that Boot
   binds with `MockMvcWebTestClient.bindToApplicationContext` when there is no `MockMvc` bean: a MockMvc without
   servlet filters, so Spring Security never runs. With `@AutoConfigureMockMvc` added, the same bearer token
   gives `viewer.name = Ada` (`mockmvc-bearer/`).

## Verified from source or by running

8. **Apollo Client 4.2.12 and `@defer`**: without `incrementalHandler` it refuses `@defer` (invariant 67); with
   `Defer20220824Handler` it sends `Accept: multipart/mixed;deferSpec=20220824,application/graphql-response+json,
   application/json;q=0.9` (`apollo-defer/`). `GraphQL17Alpha9Handler` sends `incrementalSpec=v0.2` (source).
9. **Apollo Kotlin 5.2.0** (source at the tag): Android `SqlNormalizedCacheFactory(context, name = "apollo.db",
   ...)`, JVM takes a JDBC URL; the factory is `@Deprecated` since 5.0 in favour of apollo-kotlin-normalized-cache
   (v1.0.9); optimistic updates live in `OptimisticCache`'s in-memory `recordJournals` map.
10. **PayPal**: "every round trip costs at least 700ms in network time (at the 99th percentile)" is on PayPal's
    developer blog (2018-10-16). The "3 to 52 products" figure ("Scaling GraphQL at PayPal", Medium) stays
    unverified: Medium returns 403 to automated fetches; a mirror returned 404.

## LLM and MCP runs (Claude Code 2.1.330, print mode, no tools)

11. **Lab 7.1 SDL-only test**: 50 questions on the reference schema without descriptions, 27 with them
    (`sdl-only/`).
12. **Lab 18.1, two fair runs** (schema only; schema and the ten described operations): both 10 valid, 10 answered;
    total cost 21,028 and 20,045, because both asked for `books(first: 1000)`. Both used `upcomingOnly: false`
    (the trap the authoring sample fell into). With the operations the model used `search` for names
    (`llm-1801/`).
13. **Lab 18.2, a real assistant**: Claude Code connected over Streamable HTTP with a `--mcp-config` file, listed
    eight tools; the club question took one `list_clubs` call and the answer was right (tie and visibility
    handled). "Which reader has written the most reviews?" took 20 turns and probed ids u1 to u16; right answer,
    with its own caveat (`mcp-connect/`).

## Measurements

14. **Lab 9.8, five runs at 1 GB**: medians p50/p95/p99 platform-g1 9/13/18, virtual-g1 9/12/16, platform-zgc
    11/15/19, virtual-zgc 11/15/20 ms; 0 failures; G1 pauses about 400 ms, ZGC 3 to 4 ms (`tuning/medians.md`).
    Editing `bin/course` while a long `bin/course shell` ran made bash fail at the end (`line 282: -ge: command not
    found`): bash reads a script as it runs. The runs had finished; the results are complete.
15. **Kubernetes (kind 0.33.0, node v1.37.0, kubectl 1.37.0, checksums verified)**: lab 16.4's manifest with the
    Jib image; two `rollout restart`s under four curl clients every 20 ms: 0 of 2,425 and 0 of 2,579 failed.
    Prometheus 3.16.0 plus prometheus-adapter 0.12.0 (the course's overlay: a custom-metrics APIService and one
    rule) served `graphql_cost_per_second`; lab 16.6's HPA (target lowered to 500) went 2, 4, 8, 12 in 45 s and
    back to 2 after the 300 s window (`k8s/`).
16. **Go port of lab 18.6** (Go 1.27.2 image, gqlgen v0.17.98): passes the unchanged test. Two gqlgen surprises:
    `handler.New` has introspection off, which also blocks `_service` ("federated introspection disabled"); and the
    POST transport refuses any request with an `Upgrade` header, so Java's `HttpClient` (which offers `Upgrade:
    h2c` on plain HTTP) got HTTP 400 "transport not supported" on every request. Numbers: startup 3 to 5 ms, RSS
    16 to 18 MB, p99 1 ms, a 7.7 MB static binary (`go-numbers.txt`).

## Checker and runner

17. Capstone realtime probe: graphql-transport-ws when `realtime.url` is `ws://` (Module 11's `Ws.java`); passes
    on the reference against lending's `/graphql` WebSocket.
18. `bin/course checkpoint 19` and `checkpoint 15` end to end with workspaces filled from the solutions: all labs
    pass, tags written (2 min 24 s and 1 min 24 s); `workspace/` emptied afterwards.
19. Lab 14.5's Kafka topics: `KafkaTool prune m14-reviews-` before each run; 4 leftovers deleted, then one per run.
20. GitHub Actions in labs 7.6 and 13.5 moved to checkout v7, setup-java v6, setup-node v7; both labs pass.

## The full self-check

21. `bin/course check --starters` over every module: 184 auto-graded labs, 182 green (starters failing as they
    should). m16/05 failed "recovers within 20 s": the breaker's half-open probes count calls over 300 ms as
    failures, and a just-restarted reviews JVM on a machine running the whole suite can be that slow. m19/04 failed
    because PostgreSQL refused connections. Docker was then stopped (02:01:55 IST, clean exit). Re-run on 2026-11-26
    on a quiet machine: m16/05 passed three times in three, m19/04 passed (`m16-05-rerun.txt`, `m19-04-rerun.txt`).

## Not done, and why

- GitHub token checks (m02/09, m02/12): no `GITHUB_TOKEN` in this environment.
- A real Hive target for Module 16's usage reports: needs a Hive account and token.
- Visual checks (Voyager, the capstone's web page): the user said to skip browser testing.
- PayPal's "3 to 52": see 10.
- Android (Module 12): no Android SDK; the API was checked from source instead (9).

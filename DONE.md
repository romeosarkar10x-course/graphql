# DONE.md — produced and verified

## Infrastructure (2026-11-21)

- [x] `docker/Dockerfile`: Temurin 25 (Ubuntu 24.04), Maven 3.9.16, Hurl 8.1.0, grpcurl 1.9.4, jq. Built and
      tool versions verified (`_workbench/2026-11-21-version-probe/image-tools.txt`)
- [x] `docker/compose.yaml`: reference server + Toxiproxy 2.13.0 on network `gqlcourse`
- [x] `bin/course` runner: doctor, ref, toxi, list, start, test, grade, check, checkpoint, hurl, jshell, java, shell, image
- [x] `tools/`: in-image runner, test helpers, host and image doctors, course-lib (Jackson 3.1.8, graphql-java 26.1,
      JUnit console 6.1.3; rebuilt when its pom changes), `gql` helper (validate, operations, schema-lines), GitHub schema snapshot
- [x] Reference Shelfie server (Spring Boot 4.2.0, Maven): REST `/api`, GraphQL `/graphql` + GraphiQL 6.0.1,
      gRPC :9090 with reflection, `/_course/stats` request counter; deterministic seed
- [x] Switched every build from Gradle to Maven at the user's request; syllabus edited to match
      (before-copy in `_workbench/syllabus.before-maven.md`)
- [x] Handoff docs: AGENTS, CLAUDE, CONTEXT, VERSIONS, docs/ (AGENT_GRADING, TUTOR, GROUP_PLAY, AUTHORING)

## Module 0 (4 lectures, 4 labs, 2 games)

- [x] Lectures 0.1–0.4; README; 8-question checkpoint quiz; checkpoint.json
- [x] m00/01 first red test (Hurl) · m00/02 setup doctor (Maven + Java) · m00/03 three public APIs ·
      m00/04 GraphQL from jshell + status codes · m00/05 jshell speedrun · m00/06 GitHub scavenger hunt
      (answers verified against GitHub's public schema)

## Module 1 (5 lectures, 5 labs, 1 mini-project, 2 games)

- [x] Lectures 1.1–1.5; README; checkpoint quiz; club-page contract with expected outputs for c1–c3
- [x] m01/01 card sort · m01/02 REST waterfall · m01/03 one GraphQL call · m01/04 three styles + matrix ·
      m01/05 when not to (rubric) · m01/06 latency lab (11 JUnit tests + slider page) · m01/07 card game ·
      m01/08 debate · m01/09 case-study teardown (rubric)
- [x] Extra rubric parts at the user's request: m01/01 consequences.md, m01/04 change-impact.md, m01/09
- [x] Reference measurements (c1, 700 ms latency): REST 11 requests, 4,917 B, 2,182 ms; GraphQL 1 request, 824 B, 834 ms

## Module 2 (8 lectures, 8 labs, 1 mini-project, 3 games)

- [x] Lectures 2.1–2.8; README (incl. spec-vs-graphql-java gaps); checkpoint quiz; checkpoint.json
- [x] m02/01 records · m02/02 variables vs injection · m02/03 aliases · m02/04 fragments (byte-identical) ·
      m02/05 directives · m02/06 introspection to SDL · m02/07 12 named operations · m02/08 analyzer ·
      m02/09 golf · m02/10 predict · m02/11 fix the broken query · m02/12 GitHub profile CLI (picocli, stub-tested)
- [x] Reference server: `books(titleContains:, first:)`, `search` union, `Subscription.meetupScheduled`
- [x] Simulated learner run of `bin/course checkpoint 02` tagged correctly

## Module 3 (9 lectures, 9 labs, 1 mini-project, 3 games)

- [x] Lectures 3.1–3.9; README (incl. graphql-java 26.1 vs spec table); checkpoint quiz; checkpoint.json
- [x] m03/01 schema by hand + wiring audit · m03/02 long IDs · m03/03 ISBN scalar (15 bad inputs, both
      paths) + extended scalars · m03/04 nullability lab (8 scenarios + bad-day fix) · m03/05 enums
      (legacy codes, strict vs tolerant client) · m03/06 interfaces · m03/07 sealed union (compiler test) ·
      m03/08 `@oneOf` (14 lookups, inhabitability) · m03/09 full Shelfie SDL in 5 files + coordinates lint
- [x] Games: m03/10 wireframe to schema · m03/11 nullability bets · m03/12 Voyager reveal (rubric only)
- [x] Mini-project m03/13 records-to-SDL generator (Maven, jspecify `@NonNull`)
- [x] Tools: `gql coordinates`, `gql stats`, schema directories; `/voyager.html` on the reference server;
      extended-scalars 24.0 in `tools/course-lib`
- [x] Simulated learner run of `bin/course checkpoint 03` tagged correctly

## Module 4 (6 lectures, 6 labs, 1 mini-project, 2 games)

- [x] Lectures 4.1–4.6; README (incl. changes from the plan); checkpoint quiz; checkpoint.json
- [x] m04/01 first mutations (request count checked) + serial vs parallel timing · m04/02 redesign 5
      mutations (lint) + record binder with patch semantics · m04/03 userErrors vs result union → one
      sealed type · m04/04 idempotency keys, optimistic locking, jobs (Maven, H2, 50-thread harness) ·
      m04/05 four validation layers, 21 inputs · m04/06 multipart vs pre-signed upload, 50 MB at 48 MB heap
- [x] Games: m04/07 chaos button (5 seeds × 2,000 ops) · m04/09 design critique cards (8 real signatures)
- [x] Mini-project m04/08 undo everything (command pattern, 6 scripts)
- [x] Reference server: Mutation type (createReview, createReviewTyped, deleteReview, joinClub,
      leaveClub, uploadCover, createCoverUpload, confirmCoverUpload), `X-Shelfie-User`, multipart
      convention with CSRF header, pre-signed URLs to SeaweedFS (`storage` in compose),
      `/_course/reset-data` (runner resets before every lab), `bin/course ref reset`
- [x] `notes_check` in testlib: written work must add 60+ words beyond the starter
- [x] Simulated learner run of `bin/course checkpoint 04` tagged correctly

## Module 5 (8 lectures, 9 labs, 1 mini-project, 2 games)

- [x] Lectures 5.1–5.8; README (incl. changes from the plan); checkpoint quiz; checkpoint.json
- [x] m05/01 the GraphQL object (phase timings, 15,000-token parser limit) · m05/02 fetchers, call tree,
      local context, DataFetcherResult · m05/03 async siblings (sync / pool / virtual threads) ·
      m05/04 GraphQL over HTTP, September 2026 edition (JDK HttpServer, 25-check audit.hurl; Javalin bonus) ·
      m05/05 instrumentation (per-field timing in extensions) · m05/06 error handling and masking ·
      m05/07 SDL vs code wiring, FastBuilder · m05/08 plumbing (Caffeine document cache, context, cancellation)
- [x] Mini-project m05/09 tiny server ≤150 lines: 25 audit + 15 Shelfie checks (checkpoint-05's 40)
- [x] Games: m05/10 resolver relay · m05/11 spot the bug (8 concurrency/fetcher bugs)

## Module 6 (9 lectures, 9 labs, 1 mini-project, 2 games)

- [x] Lectures 6.1–6.9; README (incl. changes from the plan); checkpoint quiz; checkpoint.json
- [x] Shared contract: Boot 4.2 base app, 14 Shelfie Hurl checks, `app-test.sh` (build, start on :4000, Hurl, stop)
- [x] m06/01 Boot setup, virtual-thread load test · m06/02 controllers (local context, @Arguments,
      ArgumentValue) · m06/03 inspection enforced at startup (JSpecify) · m06/04 tenant interceptor,
      @uppercase via type visitor · m06/05 Spring Data QBE vs hand-written (SQL logged) · m06/06 six
      exceptions, one advice · m06/07 DGS 12 + Maven codegen (graphql-java 25.0 pin) · m06/08 Quarkus
      code-first, batched @Source · m06/09 bake-off (`Bench.java`, four stacks)
- [x] Games: m06/09 bake-off leaderboard · m06/10 native image race (rubric; Quarkus native 32 ms,
      Spring native 49 ms to first answer)
- [x] Mini-project m06/11 tiny Javalin GraphQL starter (annotation scanner; audit 25/25, Shelfie 14/14,
      fail-fast on unknown fields)
- [x] `bin/course native` + `docker/native.Dockerfile` (GraalVM CE 25.0.2 with the course's Maven)

## Module 7 (7 lectures, 7 labs, 1 mini-project, 2 games)

- [x] Lectures 7.1–7.7; README (incl. changes from the plan); checkpoint quiz; checkpoint.json (labs + ADR)
- [x] `bin/course design` (`tools/design/Design.java`): lint (naming, descriptions, references, 6
      anti-pattern rules), shape, clients, diff, outage (`--no-propagation`)
- [x] `bin/course inspector` + `docker/inspector.Dockerfile` (graphql-inspector 7.0.2, pinned with overrides)
- [x] Shared contract: three screen view models, Data.java (Module 5 seed + review/meetup lookups)
- [x] m07/01 three screens to SDL (shape-checked) · m07/02 GlobalId, registry, node/nodes · m07/03
      references and a members connection with edge data · m07/04 nullability audit, 4 outages,
      @semanticNonNull · m07/05 interface vs union vs enum, plus a fourth kind · m07/06 15 changes
      classified, SchemaDiff JUnit gate + RFC #1231 check, CI workflow · m07/07 180-line bad schema refactor,
      bounded contexts
- [x] Games: m07/08 Schema Jenga (10 cards, 6 frozen clients) · m07/09 Hall of shame (rubric; 9 real
      cards from AniList and GitHub)
- [x] Mini-project m07/10 evolution simulator (6 change requests revealed one at a time) + ADR

## Module 8 (7 lectures, 8 labs, 1 mini-project, 2 games)

- [x] Lectures 8.1–8.7; README (incl. changes from the plan); checkpoint quiz; checkpoint.json (labs + error contract)
- [x] PostgreSQL 18.7 course service (compose, `needs: postgres`, doctor check, PGURL), JDBC driver in course-lib
- [x] `bin/course catalog <schema> [rows]` (`contract/Catalog.java`): deterministic catalogue, 10k / 1M rows
- [x] `contract/FeedServer.java`: newest-first feed, offset + keyset paging, posting/deleting bot, request stats
- [x] m08/01 offset paging with Pageable + drift experiment (12 duplicates, 12 gaps) · m08/02 keyset SQL on
      1,000,000 rows, cursors, both directions, EXPLAIN asserted · m08/03 Spring for GraphQL connections
      (custom EncodingCursorStrategy, HQL row values, Relay argument rules) · m08/04 filter input, sort
      enum, full-text search, jqwik properties + 300-case oracle · m08/05 error model at every depth,
      predictions · m08/06 error contract (codes, masking, errorId, interceptor, error-contract.md) ·
      m08/07 DataFetcherResult data + errors + localContext · m08/08 onError NULL/PROPAGATE/HALT
      prototype + error-aware normalized cache
- [x] Games: m08/09 Infinite scroll (bot every 20 ms, two levels) · m08/10 Error detective (6 incidents
      generated by real runs, hashed key)
- [x] Mini-project m08/11 Lanterna terminal book browser, tested on a virtual terminal

## Module 9 (8 lectures, 8 labs, 1 mini-project, 2 games)

- [x] Lectures 9.1–9.8; README (incl. changes from the plan); checkpoint quiz; checkpoint.json (labs, report, CI workflow)
- [x] Redis 8.8.4 and Varnish 9.1.0 course services (compose, `needs`, doctor, SETUP ports), `tools/varnish.py`
- [x] `bin/course clubs <schema> [scale]` (`contract/ClubData.java`), `contract/club.graphqls`, the dashboard and its expected response
- [x] m09/01 datasource-proxy counter, `extensions.sql` (1,250 statements) · m09/02 MiniLoader, 16-case suite ·
      m09/03 @BatchMapping + BatchLoaderRegistry + chained loaders (8 statements) · m09/04 jOOQ MULTISET lookahead
      (1 statement) · m09/05 four caching layers, five stale-data scenarios, hashed diagnosis · m09/06 APQ over GET,
      Caffeine, Cache-Control, Varnish hit rate · m09/07 JMH, Gatling, JFR, async-profiler, Micrometer, report ·
      m09/08 document cache, four JVM configurations, winner re-run
- [x] Games: m09/09 Performance race (baseline measured in the same run) · m09/10 Guess the query count (10 rounds)
- [x] Mini-project m09/11 N+1 detector (JUnit 5 extension, 9 harness fixtures, GitHub Actions workflow)

## Module 15 (7 lectures, 7 labs, 2 mini-projects, 1 game)

- [x] Contract: `subgraphs/` (Shelfie's four subgraphs in one jar, `/_stats`, `/_reset`, delays), `compose/` (The
      Guild's composer, Rover-style config), `fed.sh`, `stack.sh`, `PlanSteps.java`, `Load.java`, `routers/`,
      `plain/` (two plain services for Nadel), `monolith.graphqls`; `bin/course fed` (Apollo Router 2.19.0, Hive
      Router 0.3.9, sha256-checked); `checkpoint-host.sh` hook in `bin/course checkpoint`
- [x] Lectures 15.1–15.7 (Hook / Concept / Java build / Experiment / Check)
- [x] Labs: m15/01 ownership map checked against federation's rules, three-shape estimate · m15/02 Nadel gateway,
      batched `@hydrated`, a rename at the gateway by the grader · m15/03 every Federation 2 directive in SDL, the
      no-key experiment · m15/04 Spring subgraphs, `@EntityMapping` List, DataLoader and single forms, store query
      counts through Apollo Router · m15/05 composition in CI (script and workflow), five plans predicted ·
      m15/06 Apollo and Hive configs for headers, introspection, plans, timeouts; load comparison · m15/07 move
      `Review.book` under traffic with blue/green deploys, zero errors, runbook
- [x] Mini-projects and game: m15/08 team simulation, club book of the month, `docker compose up --wait` ·
      m15/09 query-plan puzzle (10 plans, leaderboard) · m15/10 accounts subgraph in Kotlin (graphql-kotlin 11)
- [x] Probes: `_workbench/2026-11-22-m15-probe/FINDINGS.md` (13 findings)

`bin/course check m15 --starters` (2026-11-22): 10 auto-graded labs, 0 problems (`_workbench/2026-11-22-m15-check-starters.log`);
m15/02 re-run after re-pinning Nadel. `docker compose up --wait` of mini-project 15.8 run by hand on the host. Full-course check still TODO.

## Module 14 (7 lectures, 7 labs, 2 mini-projects, 1 game)

- [x] Contract: `openlibrary/` (a fake Open Library from responses recorded 2026-11-22, with slow/down/garbage/
      ratelimit switches and a call counter), `KafkaTool.java` + `kafka.sh`; `bin/course token --claim k=v`
- [x] Lectures 14.1–14.7 (Hook / Concept / Java build / Experiment / Check)
- [x] Labs: m14/01 three directive wirings, a moved-directive schema, start-up refusals · m14/02 Accept-Language
      negotiation, zones and DST, decimal Money, 19 checks · m14/03 tenant from the token, RLS (forced, role per
      transaction, composite FK), per-plan schema, six attack paths and direct DB checks · m14/04 BFF over REST, gRPC
      and the fake Open Library, timeouts and a breaker · m14/05 outbox, relay, idempotent projection, redelivery and
      foreign events · m14/06 audit Instrumentation, soft delete and retention, a GDPR erase checked in API, tables,
      audit and logs · m14/07 schema from metadata, hot reload, admin endpoint, traffic during swaps
- [x] Mini-projects and game: m14/08 headless CMS (types, inputs, patches, queries, mutations generated; restart) ·
      m14/09 directive zoo (three exact directives, Friday in London, your exhibit) · m14/10 Open Library gateway
      (one call per page, cache, stale, back-off)
- [x] Probes: `_workbench/2026-11-22-m14-probe/FINDINGS.md` (18 findings)

`bin/course check m14 --starters` (2026-11-22): 10 auto-graded labs, 0 problems (`_workbench/2026-11-22-m14-check-starters.log`).
Full-course check still TODO.

## Module 13 (7 lectures, 7 labs, 1 mini-project, 2 games)

- [x] Infra: labs whose `lab.json` needs `"docker"` get the host's Docker socket (Testcontainers, 13.3);
      labs that need `"node"` get Node.js 24.22.1 from `.cache/node`, copied once from `node:24.22.1-slim`
      (`bin/course node`); `COURSE_DOCKER=1 bin/course shell`. No image change
- [x] Contract: `shelfie-app/` (Shelfie under test, layered, review feed, PIT), `shelfie-it/` (PostgreSQL, Kafka,
      Keycloak), `dgs-app/`, `digest/`, `suite-test.sh` + `mutate.py` (tests graded by planted bugs),
      `mock/MockServer.java` + `mock/MockCheck.java`, `tooling-check.mjs`, `RoundVerdicts.java`, `sync-app.sh`
- [x] Lectures 13.1–13.7 (Hook / Concept / Java build / Experiment / Check)
- [x] Labs: m13/01 Mockito unit tests and a `@GraphQlTest` slice, 8 mutants · m13/02 documents, paths, errors,
      exactly three subscription events, `DgsQueryExecutor`, 9 mutants · m13/03 Testcontainers ITs (3 mutants) and
      one Hurl contract for the Module 5 and Module 13 servers (3 black-box mutants, HTTP audit 25/25 against
      11/25) · m13/04 a `WiringFactory` for any schema (MockCheck), a screen switched from mock to Shelfie, WireMock
      (5 mutants) · m13/05 SchemaDiff with client usage, 11 scenarios, a GitHub workflow · m13/06 graphql-eslint,
      Spotless, ArchUnit, 5 violations · m13/07 DGS codegen with 3 renames that must fail compile,
      `graphql.config.yml`, a SpectaQL site
- [x] Games and mini-project: m13/08 PIT tournament (80%; reference 89%) · m13/09 Break the build (12 rounds,
      hashed key, CI's false positive in round 06) · m13/10 standalone mock-server jar, 4 schemas, MockSpec (12 Hurl
      checks)
- [x] Probes: `_workbench/2026-11-22-m13-probe/FINDINGS.md` (22 findings)

`bin/course check m13 --starters` (2026-11-22): 10 auto-graded labs, 0 problems (`_workbench/2026-11-22-m13-check-starters.log`).
Full-course check still TODO.

## Module 12 (8 lectures, 8 labs, 2 mini-projects, 1 game)

- [x] Infra: `bin/course web` (npm in `node:24.22.1-slim`) and `bin/course mobile` (`gradle:9.9.1-jdk21`, Apollo
      Kotlin's codegen is Gradle-only), both on the course network with caches in `.cache/`; no image change
- [x] Contract: `shelfie.graphqls` (reference schema copy), `chat-server/` (Module 11's chat), `Capture.java`
      (wire tap), `clients-test.sh`, `cache/` (SPEC.md, 25 JSON scenarios, `Scenarios.java`, `LiveDemo.java`)
- [x] Lectures 12.1–12.8 (Hook / Concept / Java build / Experiment / Check)
- [x] Labs: m12/01 java.net.http client, stub-server wire checks, dedupe · m12/02 shelfie-digest: sync,
      reactive, per-request user, interceptor, chat subscription across a restart · m12/03 DgsGraphQlClient
      multi-query, rename fails to compile · m12/04 SmallRye typesafe and dynamic, captured on the wire ·
      m12/05 normalised cache, scenarios 01–10, live two-page update · m12/06 Apollo Client vs Relay render
      counts in jsdom (optional, rubric) · m12/07 Apollo Kotlin airplane mode with SQLite cache and outbox
      (optional, rubric) · m12/08 Resilience4j retry and breaker, safe createReview replays under Toxiproxy
- [x] Mini-projects: m12/09 normalised cache, 25 scenarios (scenario-swap group variant) · m12/10 JLine Shelfie
      CLI, 20 completion cases and a pty Tab check (completion duel) · Game m12/11 Same screen, three clients (rubric)
- [x] Probes: `_workbench/2026-11-22-m12-probe/FINDINGS.md` (19 findings)

`bin/course check m12 --starters` (2026-11-22): 8 auto-graded labs, 0 problems
(`_workbench/2026-11-22-m12-check-starters.log`). Labs 12.6, 12.7 and 12.11 are rubric-only; 12.6 and 12.7's
reference solutions were run by hand (`web-renders.txt`, `mobile-airplane*.txt`). Full-course check still TODO.

## Module 11 (7 lectures, 7 labs, 1 mini-project, 2 games)

- [x] Infra: Kafka 4.4.0 course service (one KRaft node), PostgreSQL with `wal_level=logical`, `needs: kafka`,
      doctor check, `COURSE_PUBLISH=<port>` to publish a port for group games
- [x] Contract: `live.graphqls`, `club-page.graphql`, `Ws.java` (frames, slow reader), `Balancer.java`
      (round-robin TCP balancer with a drop switch), `Load.java` (500 pollers vs subscribers, server CPU),
      `Sql.java`, `live-test.sh`; trivia (`trivia.graphqls`, `questions.json`, `Play.java`) and tic-tac-toe
      (`tictactoe.graphqls`, `Ttt.java`)
- [x] Lectures 11.1–11.7 (Hook / Concept / Java build / Experiment / Check)
- [x] Labs: m11/01 messageSent, SubscriptionExceptionResolver · m11/02 keep-alives, reconnect with gap fill over
      WebSocket and SSE through a dropped connection · m11/03 per-subscriber filter, windowed typing, cleanup,
      takeUntil, a 10,000/s flood · m11/04 three replicas over Redis pub/sub and Kafka · m11/05 Debezium
      embedded CDC, polling vs subscriptions measured · m11/06 @defer over multipart/mixed · m11/07 decision
      matrix and pitch (rubric)
- [x] Mini-project m11/08 Live trivia (scripted game, 11 checks) · Games: m11/09 Tic-tac-toe (12 checks) ·
      m11/10 Chat stress test (three replicas, two kill -9s, shared history in Redis)
- [x] Probes: `_workbench/2026-11-21-m11-probe/FINDINGS.md` (20 findings)

`bin/course check m11 --starters` (2026-11-22, after Module 11): 9 auto-graded labs, 0 problems
(`_workbench/2026-11-21-m11-check-starters.log`; its last line is a bash read error from editing `bin/course`
while the check ran, after the result). The full-course check was not rerun (TODO).

## Module 10 (8 lectures, 8 labs, 1 mini-project, 2 games)

- [x] Infra: Keycloak 26.8.2 course service (realm `shelfie`, course signing key), `bin/course token`
      (`contract/Token.java`, JDK only), `bin/course seckit` (graphql-cop 1.15, Clairvoyance 2.5.5,
      `docker/seckit.Dockerfile`), doctor check for Keycloak, `needs: keycloak`
- [x] Contract: `secure.graphqls`, the vulnerable fork (`contract/vulnerable-shelfie/`, 12 flags, CSRF chain),
      `vulnerable.sh`, `attack.sh` (CPU and memory from /proc), `Shape.java`
- [x] Lectures 10.1–10.8 (Hook / Concept / Java build / Experiment / Check)
- [x] Labs: m10/01 resource server, 11 token cases · m10/02 29 forbidden paths, @PreAuthorize, @auth wiring,
      learner suite · m10/03 nine attacks measured, crash a node · m10/04 five limits, 8 real screens, 7 attacks,
      per-caller budget · m10/05 manifest build, trusted-documents provider, introspection off · m10/06 keys,
      scopes, Shopify-style budgets, Spender · m10/07 connection_init auth, CORS-governed origins, log
      redaction, masked errors · m10/08 graphql-cop before/after, ShapeLimits, 13 regression tests
- [x] Games: m10/09 Capture the flag (12 hashed flags) · m10/10 Red team / blue team (13 exploits, 8 functional checks)
- [x] Mini-project m10/11 Cost meter (proxy, per-minute spend, dashboard)

Partial `bin/course check --starters` (2026-11-21, after Module 10): stopped at the user's request (CPU) after
m00 to m09 and m10/01, 0 problems (`_workbench/2026-11-21-m10-check-starters.log`). Every M10 lab was tested
one by one on its solution and starter.

`bin/course check --starters` (2026-11-21, after Module 9): 96 auto-graded labs, 0 problems
(`_workbench/2026-11-21-m09-check-starters.log`).

`bin/course check --starters` (2026-11-21, after Module 8): 85 auto-graded labs, 0 problems
(`_workbench/2026-11-21-m08-check-starters.log`).

`bin/course check --starters` (2026-11-21, after Module 7): 74 auto-graded labs, 0 problems
(`_workbench/2026-11-21-m07-check-starters.log`).

`bin/course check --starters` (2026-11-21, after Module 6): 65 auto-graded labs, 0 problems
(`_workbench/2026-11-21-m06-check-starters.log`).

`bin/course check --starters` (2026-11-21, after Module 4): 44 auto-graded labs, 0 problems
(`_workbench/2026-11-21-m04-check-starters.log`). After Module 3 alone: 35, 0 problems.

`bin/course check --starters` (2026-11-21, after Module 2): 23 auto-graded labs, 0 problems
(`_workbench/2026-11-21-m02-check-starters.log`).

Earlier: `bin/course check --starters` (2026-11-21): 11 auto-graded labs, 0 problems; Module 1 re-checked after the rubric additions (`_workbench/2026-11-21-check-starters.log`).

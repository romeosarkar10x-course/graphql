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

`bin/course check --starters` (2026-11-21, after Module 7): 74 auto-graded labs, 0 problems
(`_workbench/2026-11-21-m07-check-starters.log`).

`bin/course check --starters` (2026-11-21, after Module 6): 65 auto-graded labs, 0 problems
(`_workbench/2026-11-21-m06-check-starters.log`).

`bin/course check --starters` (2026-11-21, after Module 4): 44 auto-graded labs, 0 problems
(`_workbench/2026-11-21-m04-check-starters.log`). After Module 3 alone: 35, 0 problems.

`bin/course check --starters` (2026-11-21, after Module 2): 23 auto-graded labs, 0 problems
(`_workbench/2026-11-21-m02-check-starters.log`).

Earlier: `bin/course check --starters` (2026-11-21): 11 auto-graded labs, 0 problems; Module 1 re-checked after the rubric additions (`_workbench/2026-11-21-check-starters.log`).

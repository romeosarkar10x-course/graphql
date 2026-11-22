# CONTEXT.md — locked decisions

## 1. Mission

Turn the plan in `plan/` (19 modules, 136 lectures, games, capstone) into a runnable course: lecture
notes, auto-graded labs, rubric-graded design work, games, and a reference Shelfie server, all running
in Docker.

## 2. Decisions (2026-11-21, start of implementation)

| # | Decision | The user's words |
|---|---|---|
| 2.1 | **Learner:** the user, self-study. Friends may join. | "It's for me." |
| 2.2 | **Grading: both.** Code is auto-graded (`bin/course test`: Hurl + JUnit + scripts). Design and written work (trade-off matrices, ADRs, debates, notes) is graded by an AI agent against `rubric.md`. | Picked "Both" |
| 2.3 | **First pass:** infrastructure + Module 0 + Module 1, then **stop for review**. | Picked "Infra + M0 + M1" |
| 2.4 | **Group activities ship both variants**, solo and group. | "keep both variants. i will ask my friends to join me!" |
| 2.5 | **Maven, not Gradle**, everywhere, including the syllabus text. | "i am not used to gradle. can you edit the entire syllabus to use maven!" |
| 2.6 | **Keep temporary artifacts** for reference, in `_workbench/`. | "try keeping your temporary artifacts too, for reference" |
| 2.7 | Docker is available on the build machine and is the only hard dependency for the learner. | "This machine has docker access" |

## 3. Architecture (chosen by the builder, open to review)

- **One course image** (`docker/Dockerfile`): JDK 25, Maven 3.9.16, Hurl, grpcurl, jq. Nothing else
  installs on the host. Maven's local repo lives in `.cache/m2`.
- **`bin/course` runner** modelled on the sibling `../mcp` course: `start`, `test`, `check`, `grade`,
  `checkpoint`, plus `ref` and `toxi` for services.
- **Reference Shelfie server** (`reference/shelfie-ref`): Spring Boot 4.2 serving one in-memory dataset
  three ways (REST, GraphQL, gRPC) so Modules 0 and 1 have something real to compare. It also exposes
  `/_course/stats`, a request counter labs use to check how many calls a client really made.
- **Toxiproxy** in `docker/compose.yaml` for the latency experiments (Module 1.2).
- **Single-file Java** (`bin/course java File.java`, JEP 330/458 source launch) for small client labs,
  with Jackson 3 on the class path; Maven projects from Module 2's bigger labs on.
- **Checkpoints:** `workspace/` is the learner's own git repo; `bin/course checkpoint NN` verifies a
  module's required labs and tags `checkpoint-NN`.

## 4. Adaptations of the plan

- Syllabus 0.1 says `./mvnw verify`; the course equivalent is `bin/course check --starters` (whole course)
  and `bin/course test` (one lab). Shelfie's own Maven project (from Module 5) gets `./mvnw verify`.
- Syllabus 0.2's "`setup-doctor` task" is both `bin/course doctor` (provided) and a lab where the
  learner writes their own doctor in Java.
- Syllabus 0.4: Spring for GraphQL answers GET with **405** (it does not support GET; the
  GraphQL-over-HTTP spec allows that). The lab records it as a finding, not a bug. Probe:
  `_workbench/2026-11-21-http-probe/probe.txt`.
- Versions: see VERSIONS.md (Boot 4.2.0 / Spring for GraphQL 2.1.0 / DGS 12 are newer than the plan's).
- Module 2 (2026-11-21), from probes in `_workbench/2026-11-21-m02-probe/`:
  - graphql-java 26.1 rejects operation descriptions (September 2025 grammar), so lab 2.7 uses a
    `# Description:` comment and lecture 2.7 teaches the gap.
  - Syllabus 2.5's experiment ("put `@skip` on a subscription root field; read why it is now invalid")
    cannot show an error on graphql-java 26.1: it accepts the literal form and throws a
    `NullPointerException` with a variable. The lab validates with `bin/course gql validate` and grades
    the spec-vs-implementation write-up instead.
  - Syllabus 2.2's injection payload does not parse against a `club(id: "%s")` template; the lab uses
    `books(titleContains: "%s", first: 5)` and a payload that does parse.
  - The reference schema grew (`books` arguments, `search` union, `Subscription`) so Module 2 has
    unions and a subscription to work with; the Subscription never emits until Module 11.
  - GitHub-dependent work (golf bonus holes, the CLI's live run) is optional: tests use a stub server
    and a saved schema snapshot when there is no `GITHUB_TOKEN`.
- Module 3 (2026-11-21), probes in `_workbench/2026-11-21-m03-probe/`:
  - Every lab runs graphql-java in-process (`bin/course java`, multi-file source launch for `Data.java`);
    the reference server's schema is unchanged by Module 3 (it keeps String timestamps).
  - Syllabus 3.9 "directives on directive definitions (accepted 2026)": graphql-java 26.1's parser rejects
    them; the lab records the parse error as the finding.
  - Game "Voyager reveal": the reference server serves `/voyager.html` (Voyager 2.1.0 from jsDelivr), so
    it needs a browser with internet access.
- Module 4 (2026-11-21), probes in `_workbench/2026-11-21-m04-probe/` and `-m04-storage/`:
  - The reference server became writable. `POST /_course/reset-data` restores the seed and the runner
    calls it before every lab, so earlier modules' expected outputs stay valid.
  - 4.4 uses JDBC with a version column instead of JPA `@Version` (same SQL; JPA arrives in Module 6).
  - 4.6 uses SeaweedFS for S3 (MinIO no longer publishes images) and a hand-written multipart handler in
    the reference server (Spring for GraphQL has none; the plan's DGS/multipart-spring-graphql options
    would add a second framework in Module 4).
  - The chaos button (game 4a) runs in-process against the learner's service (a harness drops responses,
    duplicates requests and races 16 clients), not through a network proxy, so runs are reproducible by seed.
- Module 5 (2026-11-21), probes in `_workbench/2026-11-21-m05-probe/`:
  - Learners' servers follow the GraphQL-over-HTTP **September 2026 edition** (422 for request errors,
    294 for partial success with `application/graphql-response+json`); lab 5.4's 25-check `audit.hurl` is
    reused by mini-projects 5 and 6. Hurl instead of the Node graphql-http audit (no Node in the image).
  - Labs 5.1–5.7 are single-file (`bin/course java`); 5.8 and the mini-project are Maven projects.
  - Every lab server is started with `exec` and killed and waited for: the runner runs all labs in one
    container, and a leaked server on :4000 broke later labs.
- Module 6 (2026-11-21):
  - Every app listens on :4000 inside the course container and is tested with Hurl through
    `modules/m06-spring/contract/app-test.sh` and the 14 shared checks in `contract/shelfie.hurl`.
  - DGS 12.1.1 is pinned to graphql-java 25.0 (VERSIONS.md). Querydsl and projected interfaces are
    explained, not built; the SmallRye typesafe client waits for Module 12.
  - Native images need GraalVM, which the course image lacks: `bin/course native` runs Maven in
    `graphql-course-native:1` (GraalVM CE 25.0.2 + Maven), and game 6b is rubric-graded.
  - Spring for GraphQL 2.1 passes 11 of the 25 HTTP audit checks; lectures say so instead of hiding it.
- Module 7 (2026-11-21):
  - One shared helper, `tools/design/Design.java` (`bin/course design`): lint rules, screen-shape check on
    mock data, client validation, SchemaDiff, outage simulation. Labs are SDL plus single-file Java.
  - graphql-inspector (Node.js) runs on the host via `bin/course inspector`; tests use SchemaDiff only.
  - `onError` is simulated with graphql-java's `@experimental_disableErrorPropagation`.
  - The evolution simulator is the mini-project (with a group variant) and holds the checkpoint's ADR.
- Module 8 (2026-11-21), probes in `_workbench/2026-11-21-m08-probe/` (`FINDINGS.md` lists 22 findings):
  - PostgreSQL 18.7 is a course service (`docker/compose.yaml`, started by `bin/course ref up`), not
    Testcontainers: the course container has no Docker socket. `needs: ["postgres"]` in lab.json.
  - `bin/course catalog <schema> [rows]` (`contract/Catalog.java`) loads a deterministic catalogue; each
    lab owns a schema (`m08_01`...) and its test reloads it, so runs are independent.
  - 8.2 is two labs (SQL keyset on 1,000,000 rows, then Spring connections); 8.3 uses Specifications, not jOOQ.
  - 8.7's onError prototype: the experimental directive added to the parsed document (NULL) and
    `ExecutionInput.cancel()` (HALT); a custom execution strategy was probed and propagates partly.
  - Spring's 400/200 statuses vs the September 2026 edition's 422/294 SHOULDs are recorded in the
    error contract, not changed.
- Module 9 (2026-11-21), probes in `_workbench/2026-11-21-m09-probe/` (`FINDINGS.md`, 19 findings):
  - Redis and Varnish are course services. Varnish has no backend until `tools/varnish.py` loads one
    over its CLI (`-S none`, admin port not published), because the app's address is per container.
  - `bin/course clubs` (`contract/ClubData.java`) is the dataset; `contract/dashboard.graphql` and its
    expected response are the yardstick every lab keeps byte for byte.
  - The contract gained `Club.ownerPicks` (a chained loader) and `Query.viewer` (per-user caching).
  - 9.5's starter is an app with five deliberate cache bugs; the experiment is the diagnosis.
  - Gatling, not k6; async-profiler from Maven Central in itimer mode; no image change for Module 9.
  - Lab 9.8's reference report uses one run per configuration and says so.
- Module 10 (2026-11-21), probes in `_workbench/2026-11-21-m10-probe/` (`FINDINGS.md`, 22 findings):
  - Keycloak 26.8.2 is a course service (no Docker socket for Testcontainers). Its realm imports a course
    signing key kept in the repository on purpose, so `bin/course token` mints expired, forged, unsigned
    and wrong-audience tokens offline; labs 10.2 on verify with the public key and need no Keycloak.
  - One shared vulnerable fork (`contract/vulnerable-shelfie/`) serves labs 10.3, 10.8, the capture the
    flag and the blue team. Its flags are computed in its source: the CTF is black box on the learner's
    honour. Its heap is capped at 512 MB and its "expensive query" flag counts field fetches, not time.
  - Labs chain: 10.1 → 10.2 → 10.4 → 10.5 / 10.6, and 10.2 → 10.7 → 10.8 (with 10.4's limits).
  - Field suggestions are a non-finding (graphql-java never suggests); Clairvoyance recovers nothing.
  - 10.7's origin check is CORS: Spring refuses cross-origin handshakes unless CORS allows the origin.
  - Scanners run in their own image (`bin/course seckit`), sharing the network namespace of the newest
    course container; the lab test re-implements graphql-cop's HIGH checks in Hurl. InQL, DVGA and ZAP are
    described, not run.
- Module 11 (2026-11-21), probes in `_workbench/2026-11-21-m11-probe/` (`FINDINGS.md`, 20 findings):
  - Kafka 4.4.0 is a course service (one KRaft node); PostgreSQL runs with `wal_level=logical` for Debezium.
  - Replicas run inside one course container on :4001-4003 behind `contract/Balancer.java` (a TCP
    round-robin balancer with a drop switch), not nginx; the same tool simulates network drops in lab 11.2.
  - Lab 11.5 uses Debezium's embedded engine in the server, not Kafka Connect.
  - Lab 11.6 serves graphql-java 26.1's own (earlier-draft) incremental payloads over a hand-written
    `multipart/mixed` route: graphql-java cannot produce RFC 1110's initial `pending`, has no `@stream`, and
    Spring for GraphQL 2.1 has no incremental transport. "First paint" is time to first byte.
  - The mini-project and the two games are graded by scripted runs; group variants use terminal clients and
    `COURSE_PUBLISH=<port>` to share a server.
- Module 12 (2026-11-22), probes in `_workbench/2026-11-22-m12-probe/` (`FINDINGS.md`, 19 findings):
  - 12.6 (web) and 12.7 (mobile) are optional, rubric-graded labs in their own images: `bin/course web`
    (Node.js 24, jsdom instead of a browser, render counts from the components) and `bin/course mobile`
    (Gradle 9.9: Apollo Kotlin's codegen has no current Maven plugin; the JVM process stands in for a phone,
    Toxiproxy for airplane mode). No Android SDK, no browser.
  - 12.6 compares Apollo Client (props, `React.memo`, data masking) with Relay; urql is lecture-only.
  - The normalised cache's contract is 25 language-neutral JSON scenarios (`contract/cache/`): lab 12.5 runs
    01–10, mini-project 12.9 all 25. The runner calls the mini-project's methods by reflection so lab 12.5's
    cache needs only write/read/extract.
  - 12.2 subscribes to a copy of Module 11's chat server (`contract/chat-server/`); the reference server's
    `meetupScheduled` never emits.
  - 12.8's faults come from a per-test Toxiproxy proxy on :8667, with the client's keep-alive off
    (Toxiproxy decides per connection). Persisted documents from clients are lecture and notes only.
  - Game "Same screen, three clients" is rubric-graded (m12/11).
- Module 13 (2026-11-22), probes in `_workbench/2026-11-22-m13-probe/` (`FINDINGS.md`, 22 findings):
  - Testcontainers runs inside the course container: `bin/course` mounts the host's Docker socket (with its
    group) only for labs whose lab.json needs `"docker"` (13.3), with a warning in the lab. Earlier modules keep
    their course services.
  - Node.js tools run inside the course image from `.cache/node` (copied once from `node:24.22.1-slim`) for labs
    that need `"node"` (13.6 graphql-eslint, 13.7 SpectaQL); `bin/course node <cmd>`.
  - Labs that teach testing grade the learner's tests by planted bugs: the learner's `src/test` runs against the
    contract app's `src/main` (`contract/suite-test.sh`, `mutate.py`), must pass unchanged and fail on each mutant.
    Game 13.8 does the same with PIT (80%).
  - 13.5 is usage-aware (breaking changes fail only when a known client operation uses them), with the clients'
    documents in the repo instead of a registry; Hive and GraphOS are described. The GitHub workflow is read by
    the test, not run.
  - IntelliJ's plugin is described; `graphql.config.yml` is checked with graphql-config. SpectaQL, not Magidoc.
  - MockSpec (RFC 0) is read server-side in mini-project 13.10; game 13.9's answer key follows the spec, and
    round 06 is a known SchemaDiff false positive.
- Module 14 (2026-11-22), probes in `_workbench/2026-11-22-m14-probe/` (`FINDINGS.md`, 18 findings):
  - Open Library is faked in tests from responses recorded on 2026-11-22 (`contract/openlibrary/`), with switches
    for slow, down, garbage and rate-limited; code points at the real API by default. Re-record each term.
  - 14.4 wraps the reference server's REST and gRPC APIs; SOAP is lecture-only.
  - 14.3's tenant is a JWT claim from Module 10's offline token tool (`--claim tenant=...`); RLS is checked by the
    grader directly in PostgreSQL. The course user is a superuser, so labs switch role per transaction.
  - 14.5's "within 10 ms" read is the mutation payload (deterministic); each test run uses its own Kafka topic.
  - 14.6's erase is verified in the API, every table, the audit log and the app's output; backups and topics are
    lecture-only.
  - Game 14.9 fixes three directives to an exact spec (auto) plus a rubric-graded exhibit; mini-project 14.10 has
    no group variant (one gateway per grader run).
- Module 15 (2026-11-22), probes in `_workbench/2026-11-22-m15-probe/` (`FINDINGS.md`, 13 findings):
  - Composition is `@theguild/federation-composition` 0.27.0 (MIT) in Node.js; Rover's composition needs the
    Elastic License 2.0 accepted, which was not done on the user's behalf (user decision pending, TODO).
  - Two routers run: Apollo Router 2.19.0 (ELv2, unlicensed: no subscriptions) and Hive Router 0.3.9 (MIT: no
    `@defer`), fetched by `bin/course fed` into `.cache/fed/bin` with sha256 checks; labs declare `"needs":
    ["fed"]`. Apollo's anonymous telemetry is left at its default (user decision pending, TODO).
  - Subgraphs are Spring for GraphQL + federation-jvm 7.0.0 (`FederationSchemaFactory`); DGS, Cosmo, Hive Gateway,
    Fusion and feddi are lecture-only. 15.2 uses Nadel outside Spring; mini-project 15.10 is graphql-kotlin 11
    with Maven, federation link pinned to v2.9.
  - `contract/stack.sh` runs the graph by hand (`SKIP=` for the learner's own subgraphs); `fed.sh` is the tests'
    helper library.
  - The checkpoint's `docker compose up` runs on the host: `bin/course checkpoint NN` now runs
    `modules/mNN-*/checkpoint-host.sh` first when it exists (only Module 15 has one).
  - 15.7's zero errors: traffic every 50 ms through blue/green deploys with Apollo's hot reload.
- Module 16 (2026-11-22), probes in `_workbench/2026-11-22-m16-probe/` (`FINDINGS.md`, 14 findings):
  - Prometheus 3.16.0, Tempo 3.1.1 and Grafana 13.3.0 run as processes inside the course image, fetched by
    `bin/course ops` into `.cache/ops` (sha256-checked, about 670 MB); labs declare `"needs": ["ops"]`. Not course
    services, so they use CPU only while a test runs. Grafana's usage reporting and update checks are off.
  - One reference app (`contract/ops-app`), one class per production feature; each lab's starter is it minus one
    feature (`_workbench/2026-11-22-m16-build/assemble.py`).
  - No Kubernetes: `ReadyBalancer.java` stands in for a Service (readiness, 1 s propagation, no retry) and
    `HpaSim.java` for the HPA (algorithm from the Kubernetes docs); manifests are checked as YAML.
  - Usage reports use Hive's v2 format and headers but go to `UsageCollector.java`; no Hive account was used.
  - The knee is measured with the app pinned to one CPU (`taskset`), as a pod limit would hold it.
  - Game day faults live in `GameDay.java` (only with `--gameday.seed`); answers are derived from the seed.
- Module 17 (2026-11-23), probes in `_workbench/2026-11-23-m17-probe/` (`FINDINGS.md`, 22 findings):
  - Spec baseline: the September 2025 edition (copy and section map in `spec/`). graphql-java 26.1 predates it
    (no descriptions on operations) and departs from it in a few lexer and grammar details, each listed in
    `contract/harness/.../Known.java` and held to the spec by the fixed cases.
  - One reference engine (`contract/engine`, Maven, no runtime dependencies); each engine lab's starter is it with
    one part stubbed (`_workbench/2026-11-23-m17-build/assemble.py`, stubs in `stubs/`). Tests compile the
    course harness against the learner's jar (`contract/harness.sh`).
  - Expected values come from graphql-java, except where the spec disagrees (block strings computed by the spec's
    algorithm; six conformance tests' locations set by hand). Validation counts compared exactly for four rules,
    "fires" for cycles and overlaps.
  - Hashed answers (17.1, 17.10): `contract/answers/answers.sh`, keys from `_workbench/2026-11-23-m17-build/gen-keys.sh`.
  - Lab 17.7's Gradle build was not run (blocked in the authoring session); the lab is rubric-graded.
- Module 18 (2026-11-23), probes in `_workbench/2026-11-23-m18-probe/` (`FINDINGS.md`, 25 findings):
  - No LLM account: lab 18.1 scores any ten queries (`agent score`); the solution's sample run was written by
    Claude Opus 5.5 while it built the lab, after seeing the data, and is labelled as such.
  - MCP is tested with the course's own client (`contract/mcp.sh`), not an assistant; connecting one is the
    learner's experiment. The lab 18.2 question changed (no reading history in the seed, which stays as it is).
  - Agent safety is server-side code (guard, approvals, audit); approvals are out of band with a token, bound to
    the reader, single use, expiring. Mini-project 18.7's starter is lab 18.3's solution with seven seeded
    weaknesses.
  - The port is TypeScript (Yoga + Pothos); Go is allowed but unverified (no reference, no Go in the image).
    The contract (`contract/port/`) is held to Java's subgraph first.
  - Viaduct's starter was not run (its Gradle wrapper was blocked in the authoring session); other platforms are
    lecture-only, licences checked on GitHub.


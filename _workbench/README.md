# _workbench

Artifacts from building the course, kept for reference (CONTEXT.md §2.6): version probes, build logs,
HTTP behaviour probes, before/after copies of edited plan files. Not course material; learners can
ignore it. One folder or file per topic, dated.

| Item | What |
|---|---|
| `2026-11-21-version-probe/` | Image build logs and tool versions inside the image |
| `2026-11-21-http-probe/probe.txt` | How the reference server answers GET, wrong content type, empty body, invalid queries, per `Accept` |
| `2026-11-21-http-probe/grpc-clubpage.json` | `GetClubPage` gRPC reply for club c1 |
| `ref-build-*.log` | Reference server builds (Gradle attempts before the Maven switch, then Maven) |
| `syllabus.before-maven.md` | The syllabus before Gradle was replaced with Maven |
| `2026-11-21-m02-probe/` | Module 2: parser and validator probes (`probe-parser.txt`, `probe-server.txt`, `validate-sub.txt`, `probe-extensions.txt`, `probe-nullable-var.txt`), Jackson 3 mapping experiments (`jackson/`), public API type counts, the September 2025 spec HTML |
| `2026-11-21-m02-check-starters.log` | `bin/course check --starters` after Module 2: 23 labs, 0 problems |
| `2026-11-21-m03-m04-versions/` | Maven Central, npm and Docker Hub lookups for Modules 3 and 4; S3 image pull attempts (`s3-images.txt`) |
| `2026-11-21-m03-probe/` | Module 3: type-system probes (`probe.txt`, `probe2.txt`: wiring, Int overflow, extended scalars, type resolvers, `@oneOf`, deprecation, directives on directives, serial vs parallel, enums, extended-validation), spec OneOf text, extended-validation/HV compatibility, Jackson enum features, starter ISBN table, experiment copies |
| `2026-11-21-m03-check-starters.log` | `bin/course check --starters` after Module 3: 35 labs, 0 problems |
| `2026-11-21-m04-storage/` | SeaweedFS S3 identity config used to probe pre-signed uploads |
| `2026-11-21-m04-probe/` | Module 4: end-to-end mutation probe of the reference server (`probe.sh`, `probe.txt`), idempotency before/after counts, upload timings and JFR heap peaks, validation error shapes |
| `2026-11-21-m04-critique/` | The 8 mutation signatures of game 4b as extracted (GitHub schema snapshot, AniList introspection) |
| `2026-11-21-m04-check-starters.log` | `bin/course check --starters` after Module 4: 44 labs, 0 problems |
| `2026-11-21-m05-probe/` | Modules 5 and 6: graphql-java phase timings, cancellation, default-handler leak, FastBuilder, StackOverflow on deep schemas, sibling timings; GraphQL-over-HTTP September 2026 copy; Spring audit (11/25), inspection, default errors, directive wiring, Spring Data SQL, virtual-thread load; Quarkus dev mode; bake-off (`bake-off.txt`), tiny starter vs raw (`tiny-vs-raw.txt`) |
| `2026-11-21-m06-native/` | Game 6b: Spring and Quarkus native build logs (GraalVM CE 25.0.2) and `race.txt` |
| `2026-11-21-m06-check-starters.log` | `bin/course check --starters` after Module 6 |
| `2026-11-21-m07-probe/` | Module 7: RFC pages and stages, graphql-java probe (error propagation off, SchemaDiff levels), graphql-inspector runs (`inspector-probe.txt`, `inspector-feed.txt`, `fifteen-changes.txt`, `jenga-deck.txt`), AniList introspection excerpts, Relay object identification spec, inspector `action.yml` |
| `2026-11-21-m07-check-starters.log` | `bin/course check --starters` after Module 7 |
| `2026-11-21-m08-probe/` | Module 8: `FINDINGS.md` (22 findings), PostgreSQL EXPLAIN runs (`bench.txt`, `deep.txt`, `orform.txt`, `count-1m.txt`, `offset-1m.txt`), Spring scroll probes (`scroll/`, `conn.txt`), Spring for GraphQL 2.1.0 sources excerpts (`sg-src/`), filter runs and the three property-test mutants (`mutants.txt`), error-model, onError and cancel probes, RFC pages and PR status (`rfcs/`), versions |
| `2026-11-21-m08-check-starters.log` | `bin/course check --starters` after Module 8 |
| `2026-11-21-m09-probe/` | Module 9: `FINDINGS.md` (19 findings), naive vs batched vs lookahead counts and timings (`naive-vs-batch.txt`, `compare.txt`, `lookahead-dashboard.sql`), chained-loader probes (`Chain.java`, `chain.txt`, `nochain.txt`), APQ and Varnish (`apq.txt`, `get.txt`, `wire.txt`), JMH, Gatling, JFR and async-profiler runs (`jmh.txt`, `gatling.txt`, `measure-run/`, `before-after.txt`, `percentile.txt`), tuning runs (`tune-256m/`, `tune-1g/`), java-dataloader failure caching, graphql-java sources excerpts (`gj-src/`), game round counts |
| `2026-11-21-m09-check-starters.log` | `bin/course check --starters` after Module 9 |
| `2026-11-21-m10-probe/` | Module 10: `FINDINGS.md` (22 findings), course key and realm, Keycloak token, auth and offline-key probes, graphql-java limits (`Limits.java`, `limits.txt`), the fork's exploit probe and attacks (`fork-probe.txt`, `attacks*.txt`, `crash.txt`), lab runs (`lab03-solution-run.txt`, `lab04-real-costs.txt`, `lab06-spender.txt`, `lab07-starter-run.txt`), WebSocket origin and close-code probes, graphql-cop and Clairvoyance output |
| `2026-11-21-m10-check-starters.log` | `bin/course check --starters` after Module 10 (partial: stopped at m10/01 at the user's request) |
| `2026-11-21-m11-probe/` | Module 11: `FINDINGS.md` (20 findings), the probe app (`app/`), WebSocket/SSE/`@defer` probes (`probe1-4*.txt`), slow-consumer floods (`probe2.txt`, `probe3.txt`, `lab03-flood.txt`), lab 11.2 frames and reconnect logs, lab 11.5 load runs (`lab05-load*.txt`), lab 11.6 timings (`lab06-defer.txt`), the stress-test run that found the reconnect race (`stress1/`) |
| `2026-11-21-m11-check-starters.log` | `bin/course check m11 --starters` after Module 11 |

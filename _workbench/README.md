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

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

`bin/course check --starters` (2026-11-21, after Module 2): 23 auto-graded labs, 0 problems
(`_workbench/2026-11-21-m02-check-starters.log`).

Earlier: `bin/course check --starters` (2026-11-21): 11 auto-graded labs, 0 problems; Module 1 re-checked after the rubric additions (`_workbench/2026-11-21-check-starters.log`).

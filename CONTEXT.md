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

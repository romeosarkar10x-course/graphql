# Module 17 — Under the hood: spec, parser and executor in Java

About 9 hours, 8 lectures. You read the GraphQL spec (September 2025 edition) and build an engine from it, part by
part: a lexer, a parser into records, six validation rules, an executor on virtual threads, introspection and an
SDL printer. Each part is tested against graphql-java 26.1, differentially and with a fuzzer, and wherever they
disagree the spec text decides. Then you read graphql-java's internals and audit Module 5's server with the GraphQL
Foundation's HTTP audit.

## Before you start

The labs need nothing beyond the course image. Lab 17.3 downloads Jazzer and graphql-java for its tests (Maven,
once). Lab 17.8 installs graphql-http with npm (Node from the course, `"needs": ["node"]`). Lab 17.7 clones
graphql-java and builds it with its own Gradle wrapper (about 3 GB).

Shared material in [contract/](contract/):

- `engine/`: **minigql**, the course's reference engine (Maven, Java 25, no runtime dependencies; about 1,500
  counted lines). Package `dev.shelfie.mini`: `Token`, `Lexer`, `Ast`, `Parser`, `Schema`, `Validator`,
  `Executor`, `Introspection`, `Printer`, `Resolver`, `MiniGraphQL`. Every engine lab's starter is this engine with
  one part stubbed; mini-project 17.9's has all of them stubbed.
- `harness/`: the course's test programs, compiled against your engine and graphql-java (`harness.sh`):
  `Cases` (fixed lexer and parser cases), `Corpus` (1,000 documents: 300 course files and 700 generated, seed 17),
  `TokenDiff`, `ParseDiff`, `ValidateDiff`, `Conformance`, `Parallel`, `Smoke`, `RoundTrip`, `IntrospectDiff`.
  `Gj` drives graphql-java; `Known` lists where graphql-java 26.1 departs from the spec, each with its finding.
  `ShelfieResolver` serves the reference server's data.
- `validation/`: 40 invalid documents and 8 tricky valid ones. `conformance/`: 30 execution tests and their fixture
  schema. `printing/kitchen-sink.graphqls`: every type-system feature.
- `http-audit/`: graphql-http 1.23.1 pinned with a lockfile, and `audit.mjs`, which runs its audits and prints one
  line per audit.
- `answers/answers.sh`: hashed answer keys for 17.1 and 17.10.

## Lectures and labs

| # | Lecture | Lab | Grading |
|---|---|---|---|
| 17.1 | [Reading the spec and the RFC process](lectures/17.1-spec.md) | [m17/01 five behaviours traced to their sections](labs/01-spec-reading/README.md) | auto + rubric |
| 17.2 | [The lexer](lectures/17.2-lexer.md) | [m17/02 sealed tokens; differential test on 1,000 documents](labs/02-lexer/README.md) | auto |
| 17.3 | [Parser, AST and fuzzing](lectures/17.3-parser.md) | [m17/03 records and pattern matching; Jazzer against graphql-java](labs/03-parser/README.md) | auto + rubric |
| 17.4 | [Validation](lectures/17.4-validation.md) | [m17/04 six rules, 40 invalid documents](labs/04-validation/README.md) | auto |
| 17.5 | [Execution on virtual threads](lectures/17.5-execution.md) | [m17/05 30 conformance tests, then Module 2's operations](labs/05-executor/README.md) | auto |
| 17.6 | [Introspection and printing](lectures/17.6-introspection.md) | [m17/06 four round trips on three schemas](labs/06-introspection/README.md) | auto |
| 17.7 | [graphql-java internals](lectures/17.7-internals.md) | [m17/07 five classes, its tests, a one-line fix, a local build](labs/07-internals/README.md) | rubric |
| 17.8 | [GraphQL over HTTP in depth](lectures/17.8-http.md) | [m17/08 the graphql-http audit](labs/08-http/README.md) | auto + rubric |

## Mini-project and games

| | Solo | Group |
|---|---|---|
| [m17/09 Build from scratch](labs/09-project-engine/README.md) (mini-project) | the whole engine in under 2,000 lines, every suite green | split along the API (lexer and parser, validation, execution, introspection), merge |
| [m17/10 Spec scavenger hunt](labs/10-spec-hunt/README.md) (game) | 15 questions against the clock | 25 minutes, one answers file per player, a leaderboard |
| [m17/11 Mock working group](labs/11-working-group/README.md) (game) | play every seat for one small RFC | champions, implementers, a client author and a spec editor decide each RFC in 12 minutes |

## Checkpoint

[Checkpoint 17](checkpoint.md): the engine (17.9), its fuzz report (17.3) and the HTTP audit (17.8).

## Changes from the plan

- **"Match graphql-java's error counts"** holds for four of the six rules. For NoFragmentCycles and
  OverlappingFieldsCanBeMerged the spec does not say how many errors to report and graphql-java 26.1 counts in its
  own way, so the test compares whether the rule fires.
- **Differential tests skip graphql-java's departures from the spec**, each one probed and listed in
  `harness/Known.java`: lone CR, U+2028 and U+2029, block strings with CR or short blank lines, descriptions on
  operations (new in the September 2025 edition), empty `{}` in SDL, and the fieldless-type lookahead. Fixed cases
  hold the learner to the spec there.
- **Conformance expectations come from graphql-java**, except six tests where graphql-java leaves `locations` off
  errors the spec says should carry them; those say so in their `graphqlJava` note.
- **"graphql-http audit to 100%"** means no MUST broken and every remaining warning and notice explained by the
  September 2026 HTTP edition: four warnings and 18 notices of graphql-http 1.23.1 ask for status codes the
  edition contradicts.
- **Lab 17.7's Gradle build was not run** while this module was written (running graphql-java's Gradle wrapper
  was not authorised in the authoring session). The commands come from reading its `build.gradle` at v26.1 and are
  marked unverified; the lab is rubric-graded.
- **Jazzer** needs the JUnit BOM import to run on JUnit 6, and `jazzer.instrument` set, or coverage stays at two
  edges.

## Spec and sources

- GraphQL, September 2025 edition: https://spec.graphql.org/September2025/ (read 2026-11-23); the draft:
  https://spec.graphql.org/draft/.
- graphql-spec CONTRIBUTING.md (RFC stages, guiding principles) and https://rfcs.graphql.org (stages read
  2026-11-23).
- GraphQL over HTTP, September 2026 edition: https://http-spec.graphql.org/September2026/.
- graphql-java 26.1 (source at tag `v26.1`), Jazzer 0.30.0, graphql-http 1.23.1: see VERSIONS.md.
- The course's probe notes: `_workbench/2026-11-23-m17-probe/FINDINGS.md`.

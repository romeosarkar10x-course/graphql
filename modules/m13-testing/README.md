# Module 13 — Testing, tooling and code generation

About 7 hours, 7 lectures. By the end every Shelfie change passes unit, integration, contract, schema-compatibility
and mutation tests, plus lint and architecture rules, and the build catches a schema rename before a client does.

## Before you start

`bin/course ref up` (labs 13.3 and 13.4 use the reference server). Two labs need more than the course image, and
`bin/course` provides it only to the labs whose `lab.json` asks:

- **Docker** (`"needs": ["docker"]`, lab 13.3): Testcontainers starts PostgreSQL, Kafka and Keycloak, so the
  course container gets the host's Docker socket for that lab's test. The socket gives control of your Docker
  daemon; it is mounted for nothing else. `COURSE_DOCKER=1 bin/course shell` mounts it by hand.
- **Node.js** (`"needs": ["node"]`, labs 13.6 and 13.7): graphql-eslint and SpectaQL. `bin/course` copies Node.js
  24.22.1 once from the official `node:24.22.1-slim` image into `.cache/node` (about 140 MB); `bin/course node
  <cmd>` runs it inside the course container.

Shared material in [contract/](contract/):

- `shelfie-app/`: Shelfie under test (Spring for GraphQL; `graph/` → `service/` → `store/`, a review feed over
  WebSocket, PIT configured). Labs 13.1, 13.2 and 13.8 grade **your tests** against this app's `src/main`;
  `sync-app.sh` keeps the labs' copies identical.
- `shelfie-it/`: Shelfie with PostgreSQL, Kafka and Keycloak, for lab 13.3. `dgs-app/`: lab 6.7's DGS slice, for
  13.2. `digest/`: a downstream client of Shelfie, for 13.4's WireMock tests.
- `suite-test.sh` and `mutate.py`: run a learner's tests against the app, then against planted bugs (mutants).
- `mock/MockServer.java`, `mock/MockCheck.java`: lab 13.4's server and the checker for any mock server's responses
  (shape, determinism, realism). `tooling-check.mjs`: loads a `graphql.config.yml` with graphql-config.
- `shelfie.graphqls`: the reference server's schema (for the mocks). `RoundVerdicts.java`: game 13.9's referee.

## Lectures and labs

| # | Lecture | Lab | Grading |
|---|---|---|---|
| 13.1 | [Unit tests](lectures/13.1-unit-tests.md) | [m13/01 Mockito for the rules, a @GraphQlTest slice, eight planted bugs](labs/01-unit-tests/README.md) | auto + rubric |
| 13.2 | [GraphQL-aware tests](lectures/13.2-graphql-tests.md) | [m13/02 Documents, paths, errors, exactly three events, DgsQueryExecutor](labs/02-graphql-tests/README.md) | auto + rubric |
| 13.3 | [Integration and contract tests](lectures/13.3-integration-contract.md) | [m13/03 Testcontainers; one Hurl contract for two servers](labs/03-integration-contract/README.md) | auto + rubric |
| 13.4 | [Mocking](lectures/13.4-mocking.md) | [m13/04 A WiringFactory for any schema, a screen on mocks, WireMock](labs/04-mocking/README.md) | auto + rubric |
| 13.5 | [Schema checks in CI](lectures/13.5-schema-checks.md) | [m13/05 SchemaDiff with client usage, operations validated, a workflow](labs/05-schema-checks/README.md) | auto + rubric |
| 13.6 | [Linting and architecture rules](lectures/13.6-lint-arch.md) | [m13/06 graphql-eslint, Spotless, ArchUnit; five violations](labs/06-lint-arch/README.md) | auto + rubric |
| 13.7 | [Code generation and dev tooling](lectures/13.7-codegen-tooling.md) | [m13/07 DGS codegen, graphql.config.yml, a SpectaQL docs site](labs/07-codegen-tooling/README.md) | auto + rubric |

Labs chain: 13.1 → 13.2 and game 13.8 (same app, your tests), 13.4 → mini-project 13.10, 13.5 → game 13.9,
13.6 → 13.7 (descriptions become docs).

## Mini-projects and games

| | Solo | Group |
|---|---|---|
| [m13/08 Mutation testing tournament](labs/08-pit-tournament/README.md) (game) | PIT mutates the services; kill 80% of its mutants | 45 minutes, highest score wins; then swap suites and raise someone else's |
| [m13/09 Break the build](labs/09-break-the-build/README.md) (game) | twelve sneaky schema changes, called before CI; then where CI is wrong | each player hides one change; points for fooling players, more for fooling CI |
| [m13/10 Schema-driven mock server](labs/10-project-mock-server/README.md) (mini-project) | one jar, any SDL, realistic deterministic data, MockSpec named mocks | schema swap: everyone writes an SDL to break the others' servers |

## Changes from the plan

- **Testcontainers runs from inside the course container** with the host's Docker socket, mounted only for labs
  that need `"docker"` (13.3). Earlier modules used course services instead because the container had no socket;
  13.3 is about Testcontainers itself, so it gets one, with a warning.
- **Node.js tools run inside the course image** from `.cache/node` (copied from the official image), so lab tests
  can run graphql-eslint (13.6) and SpectaQL (13.7). The course image itself is unchanged.
- **13.5's registry checks (Hive, GraphOS)** are described, not run (both need accounts); the lab builds the same
  idea offline: usage from the clients' documents. **graphql-inspector in GitHub Actions** is a workflow file the
  test reads, not runs; `bin/course inspector` runs the same tool locally.
- **13.6 and 13.7's IntelliJ plugin** is described: `graphql.config.yml` is checked with graphql-config, the
  library other tools use. **SpectaQL** is the docs generator (Magidoc 9.0.0 was twelve days old).
- **Labs that teach testing grade tests by planted bugs**: a learner's suite must pass on the app and fail on each
  mutant (13.1, 13.2, 13.3, 13.4), and PIT does the same at scale (13.8).
- **13.3's experiment** runs one learner-written black-box contract, Module 6's behaviour checks and Module 5's
  HTTP audit against the Module 5 server and a Spring server (Module 13's, which is Module 6's plus three fields).
- **MockSpec** (RFC 0, strawman) is a client-side spec; the mini-project reads it in a server, so any client can
  use the same mock files.

## Spec references

- [Validation](https://spec.graphql.org/September2025/#sec-Validation): validating client documents in CI (13.5);
  [Required Arguments](https://spec.graphql.org/September2025/#sec-Required-Arguments): why a non-null argument
  with a default is not required (game 13.9, round 06).
- RFC [MockSpec — GraphQL Response Mocking](https://rfcs.graphql.org/rfcs/MockSpec/) (RFC 0, strawman, opened
  2026-02-02): mini-project 13.10.
- [GraphQL over HTTP, September 2026 edition](https://http-spec.graphql.org/September2026/): the protocol contract
  Module 5's audit checks (13.3).

## Checkpoint

`bin/course checkpoint 13` (see [checkpoint.md](checkpoint.md)): a green pipeline with every gate above: labs
13.1 to 13.7, the PIT tournament at 80% and the mock server. Lab 13.3 needs Docker. Tag: `checkpoint-13`.

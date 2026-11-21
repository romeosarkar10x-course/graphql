# Module 5 — graphql-java from scratch, no framework

About 10 hours, 8 lectures. By the end you understand every moving part that Spring and DGS later hide,
because you built a spec-compliant server with only graphql-java and the JDK.

## Before you start

Nothing new to install. Every lab runs graphql-java 26.1 in-process; labs 5.8 and the mini-project are
Maven projects (`mvn -q package` inside `bin/course shell`, or just `bin/course test`). The reference
server is not needed in this module. `contract/` holds the Module 5 schema (`shelfie.graphqls`), the
seed (`Data.java`) and a plain wiring (`Shelfie.java`) that several labs share.

## Lectures and labs

| # | Lecture | Lab | Grading |
|---|---|---|---|
| 5.1 | [The `GraphQL` object](lectures/5.1-graphql-object.md) | [m05/01 Three phases, one spec map](labs/01-graphql-object/README.md) | auto + rubric |
| 5.2 | [Data fetchers in depth](lectures/5.2-data-fetchers.md) | [m05/02 Every fetcher, and what it is handed](labs/02-fetchers/README.md) | auto + rubric |
| 5.3 | [Async execution](lectures/5.3-async-execution.md) | [m05/03 Ten slow siblings](labs/03-async/README.md) | auto + rubric |
| 5.4 | [Serving over HTTP yourself](lectures/5.4-http.md) | [m05/04 GraphQL over HTTP, by the book](labs/04-http/README.md) | auto + rubric |
| 5.5 | [Instrumentation](lectures/5.5-instrumentation.md) | [m05/05 Which field is slow?](labs/05-instrumentation/README.md) | auto + rubric |
| 5.6 | [Exceptions and errors](lectures/5.6-exceptions-errors.md) | [m05/06 Five exceptions, five designs](labs/06-errors/README.md) | auto + rubric |
| 5.7 | [Wiring options](lectures/5.7-wiring-options.md) | [m05/07 One schema, two ways](labs/07-wiring/README.md) | auto + rubric |
| 5.8 | [Request plumbing](lectures/5.8-request-plumbing.md) | [m05/08 Context, cache, cancel](labs/08-plumbing/README.md) | auto + rubric |

## Mini-project and games

| | Solo | Group |
|---|---|---|
| [m05/09 A 150-line GraphQL server](labs/09-project-tiny-server/README.md) (mini-project) | JDK `HttpServer` + graphql-java + Jackson, 40 Hurl tests | One writes HTTP, one writes wiring, against the same tests |
| [m05/10 Resolver relay](labs/10-resolver-relay/README.md) | Implement a schema from names alone, then reconcile with the intent | Write schemas for each other, swap, score |
| [m05/11 Spot the bug](labs/11-spot-the-bug/README.md) | Eight broken fetchers, timed, one test each | Plant two bugs each for a neighbour |

## Changes from the plan

- **GraphQL over HTTP, September 2026 edition.** The spec's first edition was published on 2026-11-12,
  after the plan was written. Lab 5.4 follows it: 422 for validation and request-shape errors, and the
  new **294** for partial success with `application/graphql-response+json`.
- **The graphql-http audit is in Hurl.** The plan's audit suite is a Node.js tool; the course image has
  no Node, so `labs/04-http/tests/audit.hurl` has 25 checks modelled on it, each quoting its rule.
- **"No other dependency" means graphql-java plus a JSON library.** The JDK has no JSON parser; the
  mini-project allows jackson-databind and the test rejects anything else.
- **Javalin** (7.2.4) is the bonus half of lab 5.4: the same handler class behind Javalin, audited the
  same way but not graded. Module 6's mini-project uses it properly.
- **"Leaking JPA entities"** (game 5b) is played without JPA, which arrives in Module 6: bug B4 mutates
  the store's own list, the same mistake as changing a managed entity you only meant to read.
- **40 Hurl tests for checkpoint-05** are the mini-project's: 25 audit checks plus 15 Shelfie checks.

## Spec references

[Execution](https://spec.graphql.org/September2025/#sec-Execution),
[Response](https://spec.graphql.org/September2025/#sec-Response),
[Handling Execution Errors](https://spec.graphql.org/September2025/#sec-Handling-Execution-Errors),
[GraphQL over HTTP, September 2026](https://http-spec.graphql.org/September2026/).

## Checkpoint

`bin/course checkpoint 05`: labs m05/01 to m05/08 green, and the mini-project (m05/09): your own Shelfie
server passing 40 Hurl tests. Quiz: [checkpoint.md](checkpoint.md).

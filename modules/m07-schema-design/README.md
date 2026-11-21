# Module 7 — Schema design principles

About 6 hours, 7 lectures. By the end you can design a schema clients enjoy and that survives years of
change without versioning.

## Before you start

No server to run: every lab is SDL, small Java programs on graphql-java 26.1, and two new helpers.

- `bin/course design lint|shape|clients|diff|outage` (`tools/design/Design.java`): design lint (naming,
  descriptions, references and six anti-pattern rules), a screen-shape check on mock data, client
  validation, graphql-java's `SchemaDiff`, and outage simulation with or without error propagation.
- `bin/course inspector <args>`: graphql-inspector 7.0.2 in its own Node.js image (built on first use,
  `docker/inspector.Dockerfile`). Runs on the host, not inside tests.

## Lectures and labs

| # | Lecture | Lab | Grading |
|---|---|---|---|
| 7.1 | [Design from the client](lectures/7.1-design-from-client.md) | [m07/01 Three screens first, SDL second](labs/01-design-from-client/README.md) | auto + rubric |
| 7.2 | [Global object identification](lectures/7.2-global-ids.md) | [m07/02 One id for everything](labs/02-global-id/README.md) | auto |
| 7.3 | [Modelling relationships](lectures/7.3-relationships.md) | [m07/03 References, not keys](labs/03-relationships/README.md) | auto + rubric |
| 7.4 | [Nullability as a decision](lectures/7.4-nullability.md) | [m07/04 Every `!` is a promise](labs/04-nullability-audit/README.md) | auto + rubric |
| 7.5 | [Choosing abstract types](lectures/7.5-abstract-types.md) | [m07/05 One feed, three ways](labs/05-abstract-types/README.md) | auto + rubric |
| 7.6 | [Designing for evolution](lectures/7.6-evolution.md) | [m07/06 Change without versions](labs/06-evolution/README.md) | auto + rubric |
| 7.7 | [Anti-patterns and boundaries](lectures/7.7-anti-patterns.md) | [m07/07 The hall of horrors, in-house](labs/07-anti-patterns/README.md) | auto + rubric |

Shared contract: [`contract/screens/`](contract/screens/README.md) (three view models) and
[`contract/Data.java`](contract/Data.java) (Module 5's seed plus `review(id)` and `meetup(id)`).

## Mini-project and games

| | Solo | Group |
|---|---|---|
| [m07/08 Schema Jenga](labs/08-schema-jenga/README.md) | Bet on 10 cards against 6 frozen clients | Propose changes in turns; the tower gets harder |
| [m07/09 Hall of shame](labs/09-hall-of-shame/README.md) | Rank 9 real anti-patterns (AniList, GitHub), fix three | Present one card each, vote for the wall |
| [m07/10 Evolution simulator](labs/10-project-evolution/README.md) (mini-project) | 6 change requests revealed one by one, zero client breakage, then an ADR | Shipper, release-0 reviewer and product rotate |

## Changes from the plan

- **The evolution simulator is the mini-project** and carries the checkpoint's ADR; the plan lists it as
  a game. It still has a group variant.
- **graphql-inspector runs on the host** through `bin/course inspector`, in its own image: the course
  image stays Java-only, so lab tests use graphql-java's `SchemaDiff` and inspector results are recorded
  by the learner. CI (the workflow in lab 7.6) runs both.
- **`onError` is simulated** with graphql-java's `@experimental_disableErrorPropagation`, the only form
  of RFC #1163's `NULL` behaviour in graphql-java 26.1; no Java server implements the `onError` request
  property yet.
- 7.6's "graphql-inspector in CI" is a workflow file the test checks for, not a run on GitHub.

## Spec references

[Descriptions](https://spec.graphql.org/September2025/#sec-Descriptions),
[@deprecated](https://spec.graphql.org/September2025/#sec--deprecated),
[Interfaces](https://spec.graphql.org/September2025/#sec-Interfaces),
[Unions](https://spec.graphql.org/September2025/#sec-Unions);
the [Global Object Identification spec](https://relay.dev/graphql/objectidentification.htm) (Relay).
RFCs, stages as of 2026-11-21 (`_workbench/2026-11-21-m07-probe/`):
[Object Identification](https://rfcs.graphql.org/rfcs/ObjectIdentification/) (RFC 0),
[#1163 error behaviors (`onError`)](https://rfcs.graphql.org/rfcs/1163/) (RFC 1),
[Semantic Nullability](https://rfcs.graphql.org/rfcs/SemanticNullability/) (RFC 0),
[#1231 schema valid with deprecated elements removed](https://rfcs.graphql.org/rfcs/1231/) (RFC 1),
[#997 deprecating object types](https://rfcs.graphql.org/rfcs/997/) (RFC 1).

## Checkpoint

`bin/course checkpoint 07`: labs 7.1 to 7.8 and the evolution simulator green, plus the redesigned
schema (`steps/06.graphqls`) and its design decision record (`adr.md`). Quiz: [checkpoint.md](checkpoint.md).

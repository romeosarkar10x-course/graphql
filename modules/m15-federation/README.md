# Module 15 — Federation and distributed graphs on the JVM

About 8 hours, 7 lectures. One graph, many teams: stitching and hydration with Nadel, Federation 2 in SDL, Java
subgraphs on Spring for GraphQL, composition and query plans, two routers, and moving a field under traffic.

## Before you start

`bin/course fed` downloads the routers once into `.cache/fed/bin` (checksummed): **Apollo Router 2.19.0**
(Elastic License 2.0; every feature the labs use runs unlicensed) and **Hive Router 0.3.9** (MIT). Tests fetch
them on their own (`"needs": ["fed"]`). Composition is [@theguild/federation-composition](https://github.com/graphql-hive/federation-composition)
0.27.0 (MIT, Node.js from the course cache). Mini-project 15.8 and the checkpoint need Docker Compose on your
machine.

Shared material in [contract/](contract/):

- `subgraphs/`: Shelfie's four subgraphs (accounts, catalog, reviews, clubs) in one Spring Boot jar, one profile
  each, with `/_stats` (requests, `_entities` calls, last headers) and `/_reset`.
- `compose/`: the composer (`node compose.mjs supergraph.yaml [out] [api]`, a Rover-style config) and the four
  subgraphs' `supergraph.yaml`.
- `fed.sh` (test helpers: start subgraphs, compose, start either router, plans), `stack.sh` (the whole graph by
  hand: `bin/course shell -c 'bash /course/modules/m15-federation/contract/stack.sh [apollo|hive]'`),
  `PlanSteps.java` (a plan as steps), `Load.java` (latency and memory), `routers/` (course configs that expose
  plans).
- `plain/`: two plain graphql-java services for lab 15.2; `monolith.graphqls`: Shelfie before the split (15.1).

## Lectures and labs

| # | Lecture | Lab | Grading |
|---|---|---|---|
| 15.1 | [One graph, many teams](lectures/15.1-teams.md) | [m15/01 the ownership map, and the cost of one feature in three shapes](labs/01-teams/README.md) | auto + rubric |
| 15.2 | [Stitching and hydration](lectures/15.2-hydration.md) | [m15/02 a Nadel gateway, batched @hydrated, a rename at the gateway](labs/02-hydration/README.md) | auto + rubric |
| 15.3 | [Federation concepts](lectures/15.3-federation-concepts.md) | [m15/03 every Federation 2 directive in SDL; remove @key](labs/03-federation-sdl/README.md) | auto + rubric |
| 15.4 | [Java subgraphs](lectures/15.4-java-subgraphs.md) | [m15/04 FederationSchemaFactory, @EntityMapping single, List and DataLoader; count _entities](labs/04-java-subgraphs/README.md) | auto + rubric |
| 15.5 | [Composition and query plans](lectures/15.5-composition-plans.md) | [m15/05 composition in CI, five plans predicted](labs/05-composition-plans/README.md) | auto + rubric |
| 15.6 | [Routers and gateways](lectures/15.6-routers.md) | [m15/06 Apollo Router and Hive Router in production; latency and memory](labs/06-routers/README.md) | auto + rubric |
| 15.7 | [Operating a federated graph](lectures/15.7-operating.md) | [m15/07 move Review.book with zero errors; the runbook](labs/07-operating/README.md) | auto + rubric |

Labs chain: 15.1 → 15.3 → 15.4 → 15.5 → game 15.9; 15.6 and 15.7 → mini-project 15.8; 15.4 → mini-project 15.10.

## Mini-projects and games

| | Solo | Group |
|---|---|---|
| [m15/08 Team simulation: club book of the month](labs/08-project-team/README.md) (mini-project) | play all four teams, one commit each, then `docker compose up` | four people, one subgraph each, contract in schema descriptions only, plus one on the router |
| [m15/09 Query-plan puzzle](labs/09-query-plan-puzzle/README.md) (game) | ten plans from the schemas alone, 7 to pass | everyone answers in `answers/<name>.json`, the test prints a leaderboard |
| [m15/10 Polyglot preview: accounts in Kotlin](labs/10-polyglot-kotlin/README.md) (mini-project) | graphql-kotlin 11 subgraph composing with three Java ones | (solo only: one jar, one grader) |

## Changes from the plan

- **Composition uses The Guild's MIT library, not Rover or the Hive CLI.** Rover's composition asks you to accept
  the Elastic License 2.0; the course does not accept licences on your behalf. The rules and error codes are the
  same; Rover is in the lectures.
- **Two routers, both run**: Apollo Router (ELv2, unlicensed) and Hive Router (MIT). Subscriptions through Apollo
  Router need a licence (it refuses to start), so the labs use none; Hive Router has no `@defer` (rejected as an
  unknown directive). Cosmo, Hive Gateway, Hot Chocolate Fusion and feddi (a JVM-native gateway by graphql-java's
  creator, commercial) are lecture-only.
- **DGS is lecture-only** in this module: the subgraphs are Spring for GraphQL, as in every module since 6.
- **15.2's hydration is Nadel**, Atlassian's JVM engine, outside Spring (it brings its own graphql-java).
- **The polyglot preview is Kotlin with Maven**, pinned to federation v2.9 (graphql-kotlin 11 defaults to v2.13,
  which the composer does not know yet).
- **15.7's "zero errors" is measured** at one request every 50 ms through a blue/green deploy per step with the
  router's hot reload.
- **The checkpoint's compose run is on your machine** (`checkpoint-host.sh`): Docker Compose needs the host's
  paths, which the course container does not have.

## Spec references

- [Apollo Federation 2 directives](https://www.apollographql.com/docs/graphos/schema-design/federated-schemas/reference/directives)
  (saved in the workbench, v2.9 as used here): 15.3, 15.4.
- [GraphQL Composite Schemas spec](https://github.com/graphql/composite-schemas-spec/blob/main/ROADMAP.md), the GraphQL Foundation's
  vendor-neutral federation spec, roadmap at the "Proposal Stage" (2026-11-22): 15.3.

## Checkpoint

`bin/course checkpoint 15` (see [checkpoint.md](checkpoint.md)): four subgraphs and one router, `docker compose
up` in one command (mini-project 15.8), plus labs 15.3 to 15.7. Tag: `checkpoint-15`.

# Module 12 — Clients: Java first, then web and mobile

About 8 hours, 8 lectures. By the end you can consume GraphQL from Java services (by hand, with Spring's clients,
with generated types and with MicroProfile), understand normalised caching well enough to have built one, keep
a client working on a bad network, and know how web and mobile clients differ.

## Before you start

`bin/course ref up` (Shelfie and Toxiproxy). Nothing new in the course image.

- `contract/shelfie.graphqls`: the reference schema, for codegen (12.3) and the web and mobile labs.
- `contract/chat-server/`: Module 11's chat server (lab 11.1's solution), started by lab 12.2's test on :4100.
- `contract/Capture.java`: a wire tap. `bin/course java modules/m12-clients/contract/Capture.java 4999
  http://shelfie-ref:8080/graphql` prints every request body a client sends to `localhost:4999`.
- `contract/cache/`: the normalised cache contract, [SPEC.md](contract/cache/SPEC.md), 25 language-neutral
  scenarios, their runner and the live experiment.
- Two optional labs run outside the course image: `bin/course web` (npm in the official Node.js 24 image) and
  `bin/course mobile` (Gradle 9.9 with JDK 21: Apollo Kotlin's code generator is a Gradle plugin only).

## Lectures and labs

| # | Lecture | Lab | Grading |
|---|---|---|---|
| 12.1 | [What a client really does](lectures/12.1-what-a-client-does.md) | [m12/01 A minimal client on java.net.http and Jackson](labs/01-minimal-client/README.md) | auto + rubric |
| 12.2 | [Spring's GraphQL clients](lectures/12.2-spring-clients.md) | [m12/02 A second service: sync, reactive, WebSocket](labs/02-spring-clients/README.md) | auto + rubric |
| 12.3 | [Type-safe Java clients](lectures/12.3-typesafe-clients.md) | [m12/03 DgsGraphQlClient; a rename that fails to compile](labs/03-typesafe/README.md) | auto + rubric |
| 12.4 | [MicroProfile clients](lectures/12.4-microprofile-clients.md) | [m12/04 SmallRye typesafe and dynamic, in Quarkus](labs/04-microprofile/README.md) | auto + rubric |
| 12.5 | [Normalised caching](lectures/12.5-normalised-caching.md) | [m12/05 A normalised cache in 200 lines](labs/05-normalized-cache/README.md) | auto + rubric |
| 12.6 | [Web clients](lectures/12.6-web-clients.md) (overview) | [m12/06 Apollo Client vs Relay re-render counts](labs/06-web/README.md) (optional) | rubric |
| 12.7 | [Mobile clients](lectures/12.7-mobile-clients.md) (overview) | [m12/07 Apollo Kotlin in airplane mode](labs/07-mobile/README.md) (optional) | rubric |
| 12.8 | [Client resilience](lectures/12.8-client-resilience.md) | [m12/08 Retries, a breaker, safe replays under Toxiproxy](labs/08-resilience/README.md) | auto + rubric |

Labs chain: 12.1 → 12.2 → 12.3 → 12.4, 12.5 → mini-project 12.9, 12.2 → 12.8, and 12.2 + 12.6 + 12.7 → game 12.11.

## Mini-projects and games

| | Solo | Group |
|---|---|---|
| [m12/09 Build your own normalised cache](labs/09-project-cache/README.md) (mini-project) | 25 scenarios: fragments, policies, pagination, eviction, gc, optimistic layers, watches | scenario swap: everyone writes two scenarios to break the others' caches |
| [m12/10 The Shelfie CLI](labs/10-project-cli/README.md) (mini-project) | a JLine client whose Tab completion comes from introspection; 20 completion cases, a real terminal | completion duel: five tricky cases each, swapped |
| [m12/11 Same screen, three clients](labs/11-three-clients/README.md) | one feature in Spring's client, Apollo Kotlin and a web client; a one-page comparison | one client per player, a side-by-side demo, a shared page |

## Changes from the plan

- **12.6 and 12.7 are optional labs graded by rubric**, as the plan's "optional" says, and they run in their own
  images: Node.js for the web lab (the course image has no Node), Gradle for Apollo Kotlin (its codegen has no
  current Maven plugin, so this is the one place Gradle appears, as AGENTS.md allows). There is no browser and
  no Android emulator: the web screen renders in jsdom with React's own renderer and the harness counts renders;
  the "phone" is a JVM process with a SQLite cache, and Toxiproxy is the airplane-mode switch.
- **12.6 compares Apollo Client (with and without data masking and `React.memo`) and Relay**; urql is in the
  lecture only.
- **12.8's persisted documents from clients** are in the lecture and the notes, not the auto-graded part: the
  reference server has no APQ store (Module 9's labs built one).
- **12.2 subscribes to Module 11's chat server** (a copy in `contract/chat-server/`), because the reference
  server's `meetupScheduled` never emits.

## Spec references

- [GraphQL over HTTP, September 2026 edition](https://http-spec.graphql.org/September2026/) (the request and
  response a client must speak); [Response](https://spec.graphql.org/September2025/#sec-Response) (data and errors together).
- [Validation: All Variable Usages Are Allowed](https://spec.graphql.org/September2025/#sec-All-Variable-Usages-Are-Allowed)
  (why `ID` fails where `ID!` is expected, lab 12.4).
- Sources from the plan: [Spring for GraphQL 2.0 release coverage](https://www.infoq.com/news/2025/11/spring-news-roundup-nov17-2025),
  [Quarkus GraphQL clients](https://quarkus.io/blog/supersonic-subatomic-graphql/).

## Checkpoint

`bin/course checkpoint 12` (see [checkpoint.md](checkpoint.md)): two Java clients and the cache, all tested
(labs 12.1 to 12.5 and 12.8, mini-project 12.9). Tag: `checkpoint-12`.

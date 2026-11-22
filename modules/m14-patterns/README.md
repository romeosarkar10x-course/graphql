# Module 14 — Advanced schema patterns and integrations

About 8 hours, 7 lectures. The requirements most courses skip: custom directives, locales and money, tenants,
legacy backends, events, privacy, and schemas built at run time.

## Before you start

`bin/course ref up`: labs 14.3, 14.5, 14.6, 14.7 and mini-project 14.8 use the course PostgreSQL, 14.5 also Kafka,
and 14.4 the reference server's REST and gRPC APIs. Tokens are Module 10's, minted offline:
`bin/course token <user> [--claim tenant=acme]` (`--claim` is new in this module).

Shared material in [contract/](contract/):

- `openlibrary/`: a fake Open Library serving responses recorded from the real API on 2026-11-22 (the ten seed
  books' ISBNs, three searches), with switches to make it slow, down, rate-limited or nonsensical, and a call
  counter. Labs 14.4 and 14.10 test against it; your code runs against the real API unchanged.
- `KafkaTool.java` and `kafka.sh`: send and read records on the course Kafka from a test (lab 14.5).

## Lectures and labs

| # | Lecture | Lab | Grading |
|---|---|---|---|
| 14.1 | [Custom schema directives](lectures/14.1-directives.md) | [m14/01 @uppercase, @mask, @currency; stacked; refused at start-up](labs/01-directives/README.md) | auto + rubric |
| 14.2 | [Locales, time zones, money](lectures/14.2-locales.md) | [m14/02 Accept-Language, zone-safe DateTime, decimal Money](labs/02-locales/README.md) | auto + rubric |
| 14.3 | [Multi-tenancy](lectures/14.3-tenancy.md) | [m14/03 tenant from the token, row-level security, per-plan schema, six attack paths](labs/03-tenancy/README.md) | auto + rubric |
| 14.4 | [GraphQL over existing systems](lectures/14.4-existing-systems.md) | [m14/04 a BFF over REST, gRPC and Open Library, with timeouts and a breaker](labs/04-existing-systems/README.md) | auto + rubric |
| 14.5 | [Events and CQRS](lectures/14.5-cqrs.md) | [m14/05 outbox, relay, idempotent projection, PENDING in the schema](labs/05-cqrs/README.md) | auto + rubric |
| 14.6 | [Auditing, soft delete, privacy](lectures/14.6-privacy.md) | [m14/06 audit Instrumentation, soft delete and retention, a verified erase](labs/06-privacy/README.md) | auto + rubric |
| 14.7 | [Dynamic and multiple schemas](lectures/14.7-dynamic-schemas.md) | [m14/07 a schema from a metadata table, hot reload, a second endpoint](labs/07-dynamic-schema/README.md) | auto + rubric |

Labs chain: 14.1 → 14.6 (`@mask` with an owner rule) and game 14.9; 14.4 → mini-project 14.10; 14.7 → mini-project
14.8.

## Mini-projects and games

| | Solo | Group |
|---|---|---|
| [m14/08 Headless CMS in a weekend](labs/08-project-cms/README.md) (mini-project) | content types in tables become types, queries and mutations at run time, surviving restarts | content-model race: model a site, swap endpoints, build a page from introspection alone |
| [m14/09 Directive zoo](labs/09-directive-zoo/README.md) (game) | @pirate, @emojify, @redactOnFridays to an exact spec, then your own exhibit | implement someone else's directive from its description alone; stack everyone's |
| [m14/10 Open Library gateway](labs/10-project-gateway/README.md) (mini-project) | one call per page, caching, stale answers, back-off on 429 | (solo only: a team can split it, but the grader checks one gateway) |

## Changes from the plan

- **Open Library is faked in tests** (`contract/openlibrary/`, recorded 2026-11-22): tests never depend on someone
  else's API and can break it on purpose. The labs' code points at the real API by default.
- **14.4 wraps REST and gRPC; SOAP is lecture-only** (there is no SOAP service to call). The REST and gRPC services
  are the reference server's, which Module 1 introduced.
- **14.3's tenant comes from a JWT claim** minted with Module 10's offline course key (`bin/course token ... --claim
  tenant=...`), not from Keycloak, so the lab needs no identity server.
- **14.5's "within 10 ms" read is the mutation's own payload**: the projection cannot have run before the command's
  transaction committed, so the PENDING answer is deterministic, where a second request 10 ms later is not.
- **14.6's erase is verified in the API, every table, the audit and the app's own log output**; backups, topics and
  downstream copies are covered in the lecture.
- **The directive zoo's solo variant** fixes three directives to an exact spec so they can be auto-graded; the
  learner's own exhibit is rubric-graded.
- **The Open Library gateway's group variant** is left out: the grader checks one gateway's call counts, and the
  other two group activities cover the module.

## Spec references

- [Type System Directives](https://spec.graphql.org/September2025/#sec-Type-System.Directives) and "Directive Order
  Is Significant" (2.13): 14.1, game 14.9.
- RFC [#472 repeatable directives](https://rfcs.graphql.org/rfcs/472/) (accepted, merged 2020-01-10) and
  [Metadata Directives](https://rfcs.graphql.org/rfcs/wg1096/) (wg#1096, RFC 0, strawman): 14.1.

## Checkpoint

`bin/course checkpoint 14` (see [checkpoint.md](checkpoint.md)): directives, tenants and the Open Library gateway in
Shelfie: labs 14.1, 14.3, 14.4 and mini-project 14.10, plus 14.2, 14.6 and 14.7. Tag: `checkpoint-14`.

# Spec and RFC tracker

The current spec is the [September 2025 edition](https://graphql.org/blog/2025-09-08-september-edition/); five more RFCs have been accepted into the working draft since, and about 20 are actively moving. Stages below are from the [GraphQL RFC tracker](https://rfcs.graphql.org/rfcs/) as of November 2026: RFC 0 Strawman, RFC 1 Proposal, RFC 2 Draft, RFC 3 Accepted.

## Spec sections and where they are taught

| Section | Covers | Modules |
| --- | --- | --- |
| [Language](https://spec.graphql.org/September2025/#sec-Language) | Source text, documents, operations, selection sets, fields, arguments, aliases, fragments, values, variables, directives | 2, 17 |
| [Type System](https://spec.graphql.org/September2025/#sec-Type-System) | Schema, all named types, wrappers, directives, extensions | 3, 7, 14 |
| [Introspection](https://spec.graphql.org/September2025/#sec-Introspection) | Meta-fields and the introspection schema | 2, 17 |
| [Validation](https://spec.graphql.org/September2025/#sec-Validation) | Every rule a document must pass | 2, 13, 17 |
| [Execution](https://spec.graphql.org/September2025/#sec-Execution) | Requests, operations, field execution, coercion, subscriptions | 4, 5, 11, 17 |
| [Response](https://spec.graphql.org/September2025/#sec-Response) | Result map, errors, serialisation | 2, 8 |
| [GraphQL-over-HTTP](https://graphql.github.io/graphql-over-http/draft) (separate spec) | Request encoding, media types, status codes | 0, 5, 17 |

## Shipped in the September 2025 edition

| RFC | What it added | Module |
| --- | --- | --- |
| [#825](https://rfcs.graphql.org/rfcs/825/) | `@oneOf` input objects | 3 |
| [#794](https://rfcs.graphql.org/rfcs/794/) | Schema coordinates | 3 |
| [#1170](https://rfcs.graphql.org/rfcs/1170/) | Descriptions on executable documents | 2, 18 |
| [#525](https://rfcs.graphql.org/rfcs/525/), [#805](https://rfcs.graphql.org/rfcs/805/), [#1040](https://rfcs.graphql.org/rfcs/1040/), [#1053](https://rfcs.graphql.org/rfcs/1053/), [#1142](https://rfcs.graphql.org/rfcs/1142/) | Deprecation of input values; non-null `reason`; interface rules; non-null `includeDeprecated` | 3, 7 |
| [#849](https://rfcs.graphql.org/rfcs/849/) | Full Unicode range | 17 |
| [#793](https://rfcs.graphql.org/rfcs/793/), [#1056](https://rfcs.graphql.org/rfcs/1056/) | Default value coercion rules; argument coercion fix | 3, 17 |
| [#976](https://rfcs.graphql.org/rfcs/976/) | `extensions` on requests | 2 |
| [#860](https://rfcs.graphql.org/rfcs/860/), [#955](https://rfcs.graphql.org/rfcs/955/), [#1073](https://rfcs.graphql.org/rfcs/1073/) | No `@skip`/`@include` on subscription root; operation types must exist; strict error paths | 8, 11 |

## Accepted since, in the working draft

| RFC | What it changes | Module |
| --- | --- | --- |
| [#1227](https://rfcs.graphql.org/rfcs/1227/) | Allow empty selection sets | 17 |
| [#1211](https://rfcs.graphql.org/rfcs/1211/) | OneOf inhabitability (no impossible `@oneOf` types) | 3 |
| [#1183](https://rfcs.graphql.org/rfcs/1183/) | Precise "response position"; sibling errors on propagation | 8 |
| [#1206](https://rfcs.graphql.org/rfcs/1206/) | Directives on directive definitions | 3 |
| [#1193](https://rfcs.graphql.org/rfcs/1193/) | Fragments described as evolving data needs | 2 |

## Open RFCs worth following

| RFC | Stage | What it would do | Module |
| --- | --- | --- | --- |
| [#1110](https://rfcs.graphql.org/rfcs/1110/) | RFC 2 | Incremental delivery: `@defer` and `@stream` | 11 |
| [#1184](https://rfcs.graphql.org/rfcs/1184/) | RFC 2 | No sibling errors added after propagation | 8 |
| [#1224](https://rfcs.graphql.org/rfcs/1224/) | RFC 2 | Fragment arguments (needs a champion) | 2 |
| [#1058](https://rfcs.graphql.org/rfcs/1058/) | RFC 2 | List coercion algorithm | 17 |
| [#1163](https://rfcs.graphql.org/rfcs/1163/) | RFC 1 | Error behaviours: `onError` NULL / PROPAGATE / HALT | 7, 8 |
| [#1208](https://rfcs.graphql.org/rfcs/1208/) | RFC 1 | Service capabilities (what a server supports) | 18 |
| [#1231](https://rfcs.graphql.org/rfcs/1231/) | RFC 1 | A schema minus deprecated elements must stay valid | 7 |
| [#1228](https://rfcs.graphql.org/rfcs/1228/), [#1229](https://rfcs.graphql.org/rfcs/1229/) | RFC 1 | Empty object types; empty interfaces and unions | 3 |
| [#997](https://rfcs.graphql.org/rfcs/997/) | RFC 1 | `@deprecated` on object types | 7 |
| [#1127](https://rfcs.graphql.org/rfcs/1127/) | RFC 1 | Source-stream errors yield an error response | 11 |
| [#1179](https://rfcs.graphql.org/rfcs/1179/) | RFC 1 | A unified `CoerceInputValue()` | 17 |
| [#939](https://rfcs.graphql.org/rfcs/939/), [#950](https://rfcs.graphql.org/rfcs/950/) | RFC 1 | Unions that implement interfaces or include other abstract types | 3 |
| [#1200](https://rfcs.graphql.org/rfcs/1200/) | RFC 0 | Schema coordinate on errors | 8 |
| [#1230](https://rfcs.graphql.org/rfcs/1230/) | RFC 0 | `__schema(includeDeprecated:)` | 2 |
| [#1082](https://rfcs.graphql.org/rfcs/1082/) | RFC 0 | String-literal aliases | 2 |
| [Full Schemas](https://rfcs.graphql.org/rfcs/FullSchemas/) | RFC 0 | Self-contained schema documents including built-ins | 18 |
| [MockSpec](https://rfcs.graphql.org/rfcs/MockSpec/) | RFC 0 | A response-mocking specification | 13 |
| [Opt-in features](https://rfcs.graphql.org/rfcs/OptInFeatures/) | RFC 0 | Clients opting into newer behaviour | 18 |
| [Composite Schemas](https://rfcs.graphql.org/rfcs/CompositeSchemas/) | RFC 0 (separate WG spec) | Vendor-neutral federation | 15 |
| [#1065](https://rfcs.graphql.org/rfcs/1065/), [#1165](https://rfcs.graphql.org/rfcs/1165/) | RFC 0, stale | Semantic non-null type; transitional `@noPropagate` | 7 |

## Rejected or superseded — and what replaced them

| RFC | Outcome | Lesson | Module |
| --- | --- | --- | --- |
| [#395 inputUnion](https://rfcs.graphql.org/rfcs/395/) | Rejected | Solved instead by `@oneOf` | 3 |
| [#895 Client Controlled Nullability](https://rfcs.graphql.org/rfcs/895/) | Rejected | Led to the `onError` approach | 7 |
| [#1145 `@disableErrorPropagation`](https://rfcs.graphql.org/rfcs/1145/) | Rejected (2026) | Request-level `onError` preferred over a directive | 8 |
| [#888 Map type](https://rfcs.graphql.org/rfcs/888/) | Rejected | Model maps as lists of key/value objects | 7 |
| [#315 DateTime scalar](https://rfcs.graphql.org/rfcs/315/) | Rejected | Use custom scalars with `@specifiedBy` | 3 |
| [#232 `__id`](https://rfcs.graphql.org/rfcs/232/) | Rejected | Use the `Node` convention instead | 7 |
| [#1196 Field extensions](https://rfcs.graphql.org/rfcs/1196/) | Rejected (2026) | Metadata stays in directives and extensions | 14 |

## Related specs and conventions

- [GraphQL-over-HTTP](https://graphql.github.io/graphql-over-http/draft) — GraphQL Foundation, separate from the core spec
- [Composite Schemas spec roadmap](https://github.com/graphql/composite-schemas-spec/blob/main/ROADMAP.md) — vendor-neutral federation
- Apollo Federation subgraph spec — de facto standard for federation today (Module 15)
- Relay Connection and Global Object Identification conventions (Modules 7, 8)
- graphql-ws WebSocket protocol and GraphQL over SSE (Module 11)
- GraphQL multipart request convention for uploads (Module 4)

**Activity — RFC of the week:** each week one learner presents one row above in 5 minutes: the problem, the proposal, and how it would change Shelfie.

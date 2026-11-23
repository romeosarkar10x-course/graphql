# ADR 0002: The router is the edge

## Status

Accepted, 2026-11-23.

## Context

With two subgraphs, trusted documents and a cost limit cannot live in a subgraph: the router sends each
subgraph documents of its own making (query plans and `_entities` calls), which no allowlist written by
clients would contain, and a subgraph sees only its share of an operation's cost. Module 10 built both in a
single Spring server; here there are two servers and one entry point.

## Decision

Hive Router 0.3.9 (MIT) is the only address clients use. It verifies the JWT offline (`router/jwks.json`),
runs only persisted documents (`router/persisted-documents.json`), estimates cost from `@listSize` and `@cost`
and refuses above 2,000, and limits depth to 10. It propagates `Authorization` to the subgraphs. The
subgraphs listen on 127.0.0.1 only, so nothing but the router (on the same host) reaches them.

## Consequences

- The subgraphs still verify the token themselves (Spring Security in lending): a misconfigured router does
  not open the data.
- Every operation change needs `scripts/persist.sh`; CI fails if the manifest is stale.
- The router answers HTTP 200 for its refusals, with an error `code`; clients must read `errors`.
- Apollo Router can enforce a safelist too, from a persisted query list kept in GraphOS (its documentation;
  not tried here: the course runs Apollo Router unlicensed, Module 15). Hive Router reads a local file, which
  suits a repository that is checked by CI.

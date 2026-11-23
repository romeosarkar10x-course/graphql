# ADR 0004: The seed is loaded at every start; no migrations yet

## Status

Accepted for the capstone, 2026-11-23. To be replaced before real use.

## Context

The checker, the Hurl suite and the demo all depend on known data: Ada has the circular saw, Olu's ladder is
overdue, and so on. Each stage's checks start the stack fresh. A migration tool (Flyway, Liquibase) would add
a dependency and a second source of truth for the schema without helping any of that.

## Decision

Each subgraph drops and recreates its PostgreSQL schema at start (`schema.sql`, `data.sql`,
`spring.sql.init.mode=always`). Loan times in the seed are relative to the start (`now() - interval ...`), so
OPEN and OVERDUE stay true on any day.

## Consequences

- Every restart loses every loan made since the last one. This is acceptable for a demo and fatal for a
  library: the first change before real use is Flyway with the current `schema.sql` as `V1`.
- Tests and the checker are repeatable without a reset endpoint.
- The Hurl suite is still written to be re-runnable on a running stack (its keys include a `run` variable,
  and it returns what it borrows), so a second run does not need a restart.

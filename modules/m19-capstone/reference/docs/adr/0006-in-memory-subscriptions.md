# ADR 0006: Availability events from memory, one lending instance

## Status

Accepted for now, 2026-11-23.

## Context

The tool page's badge should change when the tool goes out or comes back. Events must leave lending only after
the transaction commits, or a subscriber could see a borrow that then rolled back. Module 11 showed three
ways to fan events out: in memory, PostgreSQL LISTEN/NOTIFY, and Kafka.

## Decision

A Reactor sink in lending (`Availability`), published after the commit, filtered per tool for each
subscriber. Clients subscribe through the router over server-sent events; lending also serves
graphql-transport-ws on `/graphql`. A repeated borrow (an idempotent replay) publishes nothing.

## Consequences

- Only one lending instance can run: a second one would not see the first's events. The library's traffic
  (a few borrows a minute) fits one instance with a lot of room (docs/LOAD.md).
- To scale out: publish to PostgreSQL `NOTIFY` in the same transaction (delivered only on commit) and have
  every instance `LISTEN`. No new infrastructure, which is why it comes before Kafka.
- A subscriber that connects after a change does not see it; the page reads `available` first, then
  subscribes.

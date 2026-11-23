# ADR 0001: Two subgraphs, inventory and lending

## Status

Accepted, 2026-11-23.

## Context

The screens mix two kinds of data: what the library owns (tools, specs, categories, branches) and who has what
(members, loans, availability). They change for different reasons. The catalogue changes when a librarian adds
a tool, a few times a week; lending changes on every borrow and return, and carries the privacy rules. The
capstone asks for at least two subgraphs, but the split has to be one a team would choose for itself.

## Decision

Two subgraphs. **inventory** owns `Tool`, `Category`, `Branch` and the specs; it has no authorization and no
writes. **lending** owns `Member`, `Loan`, availability and both mutations, and extends `Tool` with
`available` and `currentLoan`. `Tool` is the entity they share (key `id`); `Member` is an entity of lending
alone, so a third subgraph (say, accounts) could take it over later.

## Consequences

- inventory stays simple and cacheable; all the security-sensitive code is in one place.
- A catalogue page needs both subgraphs (the badge), so every page is two fetches through the router: the
  query plan is one inventory fetch and one batched lending `_entities` fetch, never one per tool.
- If lending is down, availability is unknown; see ADR 0003 for what the page shows then.
- A bounded-context split by data ownership, not by screens: the tool page belongs to neither team.

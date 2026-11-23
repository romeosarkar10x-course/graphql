# ADR 0003: Lending knows tools by id only

## Status

Accepted, 2026-11-23.

## Context

`borrowTool` must answer `ToolNotFound` for an id that is not a tool, and lending needs a row to hang the
"one open loan per tool" constraint on. Lending could call inventory to check the id, share inventory's
table, or keep its own list of tool ids.

## Decision

Lending keeps `tool_stock (tool_id)`, the ids it may lend, and a foreign key from `loan.tool_id` to it. It
never reads inventory's tables and never calls inventory. When inventory adds a tool, the id must be added to
`tool_stock` too (in the seed today; an event from inventory in a real deployment). Lending answers
`Tool.available` for any id: an id it does not know is simply never out.

## Consequences

- No synchronous call between subgraphs: lending is up whenever its database is.
- The two lists can drift. A tool in inventory but not in `tool_stock` shows as available but cannot be
  borrowed (`ToolNotFound`); the runbook has a query that finds such ids.
- If lending is down, nobody knows what is on the shelf. `available` is nullable so that this costs the
  badge, not the page: the catalogue loads with `available: null` and an error, and the page says "ask at the
  desk". A first version made it `Boolean!`; with lending stopped, the whole catalogue came back as
  `data: null`, because the router's error propagated up to `tools` (probe, 2026-11-23). Showing "on the
  shelf" when nobody knows would be worse than either: it is the bug members complained about.

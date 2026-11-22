# Findings: red-teaming Shelfie's MCP server

Reference notes: the seven seeded weaknesses, each with the course attack that shows it, and the fix (the
solution's code is lab 18.3's). Measured on 2026-11-23: the starter blocks 6 of the 12 course attacks
(`_workbench/2026-11-23-m18-probe/mcp-server/redteam-starter.txt`); the solution blocks all 12 and its two own.

## The seven

| Weakness | Attack | Cause | Fix |
| --- | --- | --- | --- |
| `last` is free | A01: `books(first: 5) { reviews(last: 900) { body } }` passes as cost 15; priced properly it is 9,005 | `QueryGuard.COST` reads only `first` | count `last` like `first` |
| Only `__schema` is introspection | A02: `__type(name: "Mutation") { fields { name args { name } } }` maps the write API | name check `equals("__schema")` | refuse every `__` field except `__typename` |
| Approvals not bound to the reader | A03: u8 confirms u6's approved request; Shelfie runs it as u8 | `take` never compares `r.user` | refuse unless the caller asked for it |
| No expiry | A04: a request approved and confirmed after its 3 s TTL still runs | `take` never checks `expires` | refuse after `expires` |
| Reusable approval | A05: confirm, undo by hand, confirm the same id again | `APPROVED` never moves to `DONE` | mark it `DONE` in the same synchronized step |
| Token prefix | A06: `X-Approver-Token:` empty, or its first three characters, approves | `token.startsWith(given)` | `MessageDigest.isEqual` on the whole token |
| Writes on by default | the test's first check: write tools without `shelfie.mcp.writes` | `matchIfMissing = true` and `writes: true` | off unless asked for |

The approval bugs compound: with A03 and A05 together, any signed-in reader can replay any approved request as
themselves, for ever.

## My attacks

- **M01, two readers in one header** (`X-Shelfie-User: u4, u1`). A server that splits on commas, or forwards the
  header unparsed to a backend that takes the first or last value, acts as someone else. `Caller` accepts only
  `u` and digits, so the write is refused.
- **M02, a user id as an extra argument** (`join_club {"clubId": "c2", "userId": "u1"}`). A server that copies
  tool arguments into variables wholesale would forward it. Spring AI's input schema has
  `additionalProperties: false`, and the call fails validation.

## Why these layers

Cost and introspection belong in the guard because they are properties of the document, decided before anything
is sent. Ownership, expiry and single use belong in the approval state machine, checked atomically at confirm
time, because they are properties of the request over time. The token check is the human's door, and the
default belongs in configuration so a forgotten flag fails closed.

## What a real deployment adds

The MCP client authenticates (the MCP authorization spec's OAuth flow, or Module 10's JWTs) and the reader is
derived from a verified token, not a header. Rate limits per reader on tool calls and on approval requests (an
agent that files fifty requests is itself a signal). The audit log goes to the same pipeline as Module 16's logs,
with an alert on bursts of `denied`. Approvals show the human the operation's description and the exact
variables, not only a summary the agent wrote.

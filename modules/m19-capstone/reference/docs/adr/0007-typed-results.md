# ADR 0007: Expected failures are typed results, not errors

## Status

Accepted, 2026-11-23.

## Context

"Someone else has it until Friday" and "you have three tools out" happen every day and the page must show them
well: when the tool is back, how many is the limit. GraphQL errors are untyped (a message and free-form
extensions), are easy for a client to drop, and in a federated response carry the router's `code` and the
subgraph's name. Module 4 and Module 8 compared payloads with `userErrors` and result unions.

## Decision

Result unions: `BorrowResult = Borrowed | ToolUnavailable | LoanLimitReached | ToolNotFound | InvalidInput`
and `ReturnResult = Returned | LoanNotFound | AlreadyReturned`. Every failure implements
`interface Problem { message: String! }`. Errors are kept for mistakes a correct client does not make (a page
of 51, a stale cursor) and for access (UNAUTHORIZED, FORBIDDEN).

## Consequences

- Clients select `... on Problem { message }` and show any failure, including ones added later; a new union
  member is a dangerous (not breaking) change, and clients without a case for it still have the message.
- The Java client switches over a sealed type; the compiler lists the cases it misses.
- Hurl and the checker can assert on `__typename`, which is stabler than an error message.
- Someone else's loan is `LoanNotFound`, not FORBIDDEN: a member cannot tell another member's loan id from
  one that does not exist.

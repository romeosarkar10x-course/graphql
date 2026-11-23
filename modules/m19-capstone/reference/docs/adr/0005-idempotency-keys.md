# ADR 0005: Idempotency keys for borrowing, stored with the loan

## Status

Accepted, 2026-11-23.

## Context

A member taps Borrow on a phone on a bad connection; the request reaches the server, the response is lost, and
the app retries. Without care that is two loans, or a `ToolUnavailable` for the tool the member now has.
Returning needs no key: returning a returned loan already answers `AlreadyReturned` and changes nothing.

## Decision

`BorrowInput.idempotencyKey` is required (1 to 64 characters, a new UUID per tap; the client reuses it on a
retry). Inside the borrow transaction lending locks the member's row, looks up `(member, key)` in
`borrow_request`, and either replays the stored answer or decides and stores it, in the same transaction as
the loan. Failures are stored too, so a retry of a `LoanLimitReached` is still `LoanLimitReached` even if a
tool was returned in between. A replayed `Borrowed` reads the loan again, so it shows the loan as it is now.

## Consequences

- Two identical requests: one loan, two identical answers (the checker's idempotency probe, and
  `LendingGraphQlTest`).
- Keys are per member: another member sending the same key gets their own answer (attack A15).
- Keys are kept forever; a real deployment would delete rows older than a day.
- An invalid key cannot be stored, so `InvalidInput` for the key itself is not replayed (nothing to replay).

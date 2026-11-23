# Stage 1 review: Toolshed's contract

Solo variant, as an example: written by the course's authoring agent acting as the peer, against the
capstone's `design-review.md` rubric, after the contract had been probed and tested. Each point says how the
problem was actually found. The author's answers follow each point; changes are in this repository.

## What works

- **Client-first.** Every operation maps to a screen or a job in DESIGN.md's table. `Query.loan` is the only
  root field no operation uses: the author keeps it for the "loan detail" link in the reminder email, which is
  not built yet. It should say so in its description, or wait until the email exists.
- **Failures as data.** `BorrowResult` covers what goes wrong at the desk, each failure with what the page
  needs (`expectedBack`, `limit`, `field`), and `Problem` lets an old client show a new failure's message.
- **Abstract types chosen for a reason.** `ToolSpecs` is an interface (they share `summary`), `SearchResult` a
  union (nothing shared): the right way round (Module 7.5).
- **`@oneOf` earns its place.** The desk has a barcode, a link has an id; one input with exactly one of them,
  checked by validation, instead of two nullable arguments and a rule in a description.

## Points

1. **`Tool.available: Boolean!` crosses a subgraph boundary.** If lending fails, how far does the null travel?
   *Author:* checked by stopping lending: the error propagated up to `tools` and the whole
   catalogue was `data: null`. Changed to nullable; the page shows "ask at the desk". (Found by that
   outage probe, 2026-11-23.)
2. **`Loan.member` and `Member.email` are nullable.** Is that deliberate? *Author:* yes: a member at the desk
   view gets the loan with `member: null` and FORBIDDEN, and still learns the tool is out. Non-null would
   erase the loan. Documented in DESIGN.md, "Nullability".
3. **`toolId` and `loanId` on the not-found types** fail Module 7's references rule. *Author:* kept: the thing
   does not exist, so there is no object to return. Explained in DESIGN.md.
4. **Descriptions.** The first contract described 6 of its 34 types. *Author:* every type has one now (the
   checker counts them); fields are described where the name does not say it all.
5. **Cursors and sort orders.** Can a cursor from one sort order be sent with another? *Author:* the first
   implementation accepted it (both cursors had two parts); `InventoryGraphQlTest` caught it, and cursors now
   name their sort order.

## Questions for the demo

- What happens to an open loan if inventory deletes the tool? (Today inventory has no delete; say so.)
- Why is `idempotencyKey` required rather than optional? (ADR 0005.)

## Verdict

Ready for stage 2 after points 1 and 4. Points 2 and 3 are argued; point 5 is a test to keep.

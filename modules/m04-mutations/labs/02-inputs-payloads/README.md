# Lab 4.2 — Redesign five mutations

**Lecture:** [4.2 Inputs and payloads](../../lectures/4.2-inputs-payloads.md) · **Grading:** auto + rubric · **Needs:** nothing

`bad.graphqls` holds five first drafts, each wrong in a different way. Redesign them, then model the
inputs as Java records and write the binder that turns graphql-java's argument maps into them.

## The rules (the test's lint checks them)

1. camelCase, **verb first**: `createReview`, `updateClub`, `joinClub`, `leaveClub`,
   `setShelfStatus`, `deleteReviews` (use these six names; `doMembership` becomes two mutations).
2. Exactly **one argument, `input: <Name>Input!`**, where `<Name>` is the mutation name capitalised.
3. Returns **`<Name>Payload!`**, which has `userErrors: [UserError!]!` **and** at least one data field
   (what changed, so client caches can update without a second request).
4. `UserError { field: [String!] message: String! code: <an enum> }`.
5. No identity in inputs: no `userId`, `authorId`, `viewerId` (the server knows who is calling), and no
   `clientMutationId` (Relay legacy, lecture 4.2).
6. Closed sets are enums: an input field called `status`, `action`, `visibility`, `kind` or `type` must
   be an enum.

## Do

1. `redesign.graphqls`: the six mutations and their types. Run the lint yourself:
   `../../../bin/course java ../../../modules/m04-mutations/labs/02-inputs-payloads/tests/Lint.java redesign.graphqls`
2. `Inputs.java`: a record per input, plus `Visibility` and `ReadingStatus` enums. `UpdateClubInput`
   is a **patch**: absent fields stay as they are, `null` clears. Use `Optional<T>` components:
   `null` reference = absent, `Optional.empty()` = sent as null, `Optional.of(v)` = set.
3. `Bind.java`: `bind(map, RecordClass)` for Strings, ints, booleans, enums, nested records, lists and
   the three-state `Optional`; a primitive with no value is an `IllegalArgumentException`.
4. `notes.md`: one paragraph per original mutation.

## Done when

`bin/course test m04/02` is green and `bin/course grade m04/02` gives 7/10 or more.

# Reading the spec (reference notes)

## Five behaviours

### subscription-skip: 5.2.4.1 Single Root Field

`CollectSubscriptionFields`: "For each selection in selectionSet: selection must not provide the @skip directive.
selection must not provide the @include directive." Then "collectedFieldsMap must have exactly one entry, which
must not be an introspection field". A server that accepts `@skip` there skipped the first step: whether a
subscription has one root field must be known without the variables.

### two-operations: 6.1 Executing Requests (GetOperation)

"If operationName is null: If document contains exactly one operation. Return the Operation contained in the
document. Otherwise raise a request error requiring operationName." A request error: no data at all (7.1.3).

### alias-collision: 5.3.2 Field Selection Merging

`FieldsInSetCanMerge(set)`: for fields with the same response name whose parent types are equal (or not both
object types), "fieldA and fieldB must have identical field names" and identical arguments. `name: description
name` fails the first.

### null-propagation: 6.4.4 Handling Execution Errors

"If an execution error is raised while resolving a field (either directly or nested inside any lists), it is
handled as though the response position at which the error occurred resolved to null", and a null in a non-null
position propagates to the parent. A server that left the null in place skipped CompleteValue's non-null check.

### int-range: 3.5.1 Int

Input coercion: "If the integer input value represents a value less than -2^31 or greater than or equal to 2^31, a
request error should be raised." So `3000000000` as an Int literal is a validation error and as a variable a
request error; as a result value it is a field error.

## The RFC process

1. Stage 0 Strawman (a described problem, no entrance criteria), 1 Proposal (a champion, the problem and solution
   with examples, incomplete spec edits, the known drawbacks), 2 Draft (working-group consensus that this solution
   is preferred, precise spec edits, a compliant GraphQL.js implementation that might not be merged yet), 3 Accepted
   (complete spec edits and a tested GraphQL.js implementation; an editor merges it and the next edition includes
   it), and X Rejected. Stages usually move at Working Group meetings, with the champion bringing the RFC
   (graphql-spec CONTRIBUTING.md).
2. Stage 1: #1179 `CoerceInputValue()` (input coercion rewritten as composed algorithms, with a "coercion failure"
   each caller turns into its own kind of error). Stage 3: #1227 empty selection sets
   (`{ }` becomes valid; merged into the draft on 2026-09-17, after the September 2025 edition).
3. Older than September 2025: it rejects descriptions on operations, which that edition added (RFC #1170).

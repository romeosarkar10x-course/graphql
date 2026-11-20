# Checkpoint 02 quiz

Answer without running anything, then check.

1. `{ club(id: "c2") { members { name } } }` returns how many members, and why?
2. What is wrong with `String.format("{ book(id: \"%s\") { title } }", input)`, beyond quotes breaking it?
3. `{ x: club(id: "c1") { name } x: club(id: "c2") { name } }`: what happens, and at which stage?
4. A refactor into fragments returns the same data but a snapshot test fails. Most likely cause?
5. Where may `@include` not appear according to the September 2025 spec, and what does graphql-java 26.1 do there?
6. `members: [User!]!` in introspection: write the chain of `kind`s down to the name.
7. A document has two operations and the request has no `operationName`. What happens?
8. Why does an analyzer have to expand fragments before counting fields?

<details><summary>Answers</summary>

1. All 4: the argument default `first: Int = 10` applies, and c2 has 4 members.
2. Input becomes syntax: a payload can change limits, add fields such as `__schema`, or comment out the
   rest of the document. Variables keep the document constant.
3. Validation rejects it with `FieldsConflict` (different arguments under one response key); nothing executes.
4. Field order changed: fields come back in the order they are first selected, and a spread
   contributes its fields where it stands.
5. In a subscription's root selection set (rule "Single Root Field"). graphql-java 26.1 accepts a
   literal `@skip(if: false)` there and crashes with a `NullPointerException` when the value is a variable.
6. `NON_NULL > LIST > NON_NULL > OBJECT (User)`.
7. Error: `Must provide operation name if query contains multiple operations.`
8. The server executes the expanded tree; a fragment spread three times costs three times.
</details>

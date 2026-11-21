# Checkpoint 7 quiz

`checkpoint-07` is the redesigned schema (mini-project 7's `steps/06.graphqls`) plus its design decision
record (`adr.md`), with labs 7.1 to 7.8 green.

1. A screen needs `memberPreview` and your field is `members(first:)`. Where does the screen's name go?
2. Why are Relay global ids base64, if base64 hides nothing?
3. When is a list field the wrong choice even though today's data is small?
4. Give the three-question nullability checklist, and say what `@semanticNonNull` adds.
5. A feed will gain new kinds every quarter. Interface, union or enum discriminator, and why?
6. Classify: (a) output `Book` to `Book!`, (b) argument `Int` to `Int!`, (c) a new enum value, (d) a
   changed argument default.
7. SchemaDiff says BREAKING but the frozen clients all validate. Name the two ways that can happen.
8. Why is `getNotifications(user_id: Int!)` an anti-pattern beyond its naming?

<details><summary>Answers</summary>

1. In the query, as an alias: `memberPreview: members(first: 5)`. The schema names suit every client.
2. To signal "opaque: don't parse or build these", so the server can change the encoding without
   breaking clients that only pass ids back.
3. When it can grow without bound, needs paging, or will need data about the pair: switching a list to
   a connection later is a breaking type change.
4. Absent in correct data? Nullable. Served by another dependency than the parent? Nullable (it can fail
   alone). Otherwise non-null. `@semanticNonNull` marks the second kind when the value is never absent
   in correct data: null then means only "an error happened".
5. Interface: shared fields (actor, time) let old clients render new kinds generically; SchemaDiff
   calls a new implementation safe.
6. (a) safe, (b) breaking, (c) dangerous, (d) dangerous.
7. The removed or changed coordinate is used by no frozen client (an unused field), or every client
   already satisfies the stricter shape (they all pass the newly required argument).
8. Identity comes from the client: anyone can read anyone's notifications. The caller belongs to the
   request (`viewer`), never to an argument.
</details>

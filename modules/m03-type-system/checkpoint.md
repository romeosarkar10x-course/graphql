# Checkpoint 03 quiz

Answer without running anything, then check.

1. Which graphql-java step would catch a type resolver missing for an interface, and which step never
   notices a missing data fetcher?
2. A field returns a Java `long` of 5,000,000,000 and is declared `Int`. What does the client get?
3. Your ISBN scalar accepts hyphens in variables but not in literals. Which method is wrong?
4. `club: Club`, `owner: User!`, `owner.name: String!` throws. What is `data`?
5. You add `ABANDONED` to an output enum. Who breaks?
6. Why must `type Book implements Reviewable & Node` repeat `Node`?
7. `input A @oneOf { a: A }`: what does graphql-java do, and why?
8. Write the coordinate of the `reason` argument of `@deprecated`.

<details><summary>Answers</summary>

1. `SchemaGenerator.makeExecutableSchema` rejects the missing type resolver. Nothing checks for a
   missing data fetcher: the default property fetcher returns null.
2. A field error (`Can't serialize value ... Expected a value that can be converted to type 'Int'`)
   and null at the nearest nullable ancestor.
3. `parseLiteral` (literals) disagrees with `parseValue` (variables); both should call one normalise.
4. `{"club": null}`: the error bubbles through `owner: User!` to the nullable `club`.
5. Clients that decode the enum into a closed type without an unknown fallback.
6. The spec requires implementers to list every interface transitively implemented.
7. Rejects the schema at build: the oneOf input is uninhabited (no finite value exists), RFC #1211.
8. `@deprecated(reason:)`.
</details>

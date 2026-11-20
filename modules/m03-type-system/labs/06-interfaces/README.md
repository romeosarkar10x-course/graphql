# Lab 3.6 — Interfaces and type resolvers

**Lecture:** [3.6 Interfaces](../../lectures/3.6-interfaces.md) · **Grading:** auto + rubric · **Needs:** nothing

A field of interface type returns *some* object type. graphql-java cannot guess which: a
`TypeResolver` looks at the Java value and names the GraphQL type. No resolver, no schema.

## Do

1. Run the starter with any query and read the startup error. Write it in `notes.md`.
2. **TODO 1:** one `TypeResolver` for `Node`, `Timestamped` and `Reviewable`, using a pattern-matching
   `switch` over the `Data` records. Register it for each interface.
3. **TODO 2:** `Query.node`, `nodes`, `reviewables` and `recent`, as the schema's descriptions say.
4. Experiments in `notes.md`.

Try `{ node(id: "c1") { __typename id ... on Timestamped { createdAt } } }`: fragments on an interface
work on any type that implements it.

## Done when

`bin/course test m03/06` is green and `bin/course grade m03/06` gives 7/10 or more.

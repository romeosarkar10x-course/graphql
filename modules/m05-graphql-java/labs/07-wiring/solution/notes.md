# Lab 5.7 notes

## Build time for 2,000 types (ms, third round)

Course image, 2026-11-21:

| | sdl | builder | fast |
|---|---|---|---|
| 2,000 types | 41.7 | 25.0 | 1.3 |

A first try chained every type to the next (`next: T<i+1>`): SchemaGenerator recursed once per link and
threw `StackOverflowError` from 1,000 types on (`_workbench/2026-11-21-m05-probe/schema-chain-stackoverflow.txt`).

## Questions

1. Shelfie: SDL. The schema is the contract other teams and clients read and review in pull requests,
   and it diffs cleanly. A catalogue-driven tool: builders (and FastBuilder if startup matters), since the
   types only exist at runtime and generating SDL text just to parse it again is wasted work.
2. In both, in the `GraphQLCodeRegistry`, keyed by field coordinates: `RuntimeWiring` fills it for SDL,
   and I filled it by hand for the builders. Keeping fetchers out of the types keeps types immutable and
   shareable: the same type definitions can be wired differently (tests, mocks, the public view) and
   transformed without touching behaviour.
3. The full traversal of the type graph: it does not collect types reachable from the roots, replace
   type references by walking everything, or run the schema validation rules unless you call `withValidation(true)` (off by default in 26.1; checked with javap). If a type is
   missing or two types share a name, the problem shows up at query time, or never as a clear error.
4. Applying a naming convention or `@deprecated` across a schema; stripping fields a tenant has not paid
   for; adding a field-level directive that turns on auth checks; producing a federation subgraph's
   view of a larger schema.

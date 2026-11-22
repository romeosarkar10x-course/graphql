# Design (reference solution)

## Shape

1,500 counted lines in twelve files. A request: `MiniGraphQL.execute` → `Parser.parse` (which calls `Lexer.lex`) →
`Validator.validate` → `Executor.execute`, which calls the `Resolver` for user fields and `Introspection` for
`__schema`, `__type` and the introspection types.

| Part | File | Lines |
| --- | --- | --- |
| Tokens and lexer | Token, SyntaxError, Lexer | about 200 |
| AST and parser | Ast, Parser | about 380 |
| Schema | Schema (with the built-ins and introspection types as SDL) | about 170 |
| Six rules | Validator | about 260 |
| Execution | Executor, Resolver | about 330 |
| Introspection and printing | Introspection, Printer | about 190 |

## Choices

- **The AST is the schema.** `Schema` keeps the SDL's own records (ObjectDef, FieldDef...) and the introspection
  types are SDL parsed by the same parser, so introspection is ordinary execution over the schema's records.
- **Errors are exceptions only at the edges.** The lexer and parser throw `SyntaxError`; validation collects
  `Problem`s; execution catches resolver exceptions per position (`guarded`) and propagates nulls with one private
  `Bubble` exception, the error already recorded, so the spec's 6.4.4 is one method.
- **One virtual thread per field and per list item** for queries, joined in order so the response keeps the
  document's order. Mutation root fields run in a loop.

## Where we differ from graphql-java

Lexer and parser: lone CR, U+2028/U+2029, block strings with CR or short blank lines, descriptions on operations,
empty braces, the fieldless-type lookahead (see `_workbench/2026-11-23-m17-probe/FINDINGS.md`). Execution: we
give locations on result-coercion errors and on nulls in non-null positions; graphql-java does not. Validation:
error counts for cycles and overlapping fields (implementation-defined). Introspection: we do not add `@defer`;
`includeDeprecated` is `Boolean!` as in the September 2025 edition.

## What we left out

The other 20-odd validation rules (the executor assumes valid documents), subscriptions, custom scalar coercion,
limits (depth, tokens, complexity: graphql-java 26 has defaults), DataLoader-style batching, instrumentation, and
parse and validate caching.

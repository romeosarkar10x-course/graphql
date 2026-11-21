# Lab 5.7 — One schema, two ways

**Lecture:** [5.7 Wiring options](../../lectures/5.7-wiring-options.md) · **Grading:** auto + rubric · **Needs:** nothing

`small.graphqls` is a cut-down Shelfie (Query, Club, User, an enum and an `@internal` directive).
`TwoWays.java` builds it from SDL already. Build it again from Java, transform it, and time both.

| Command | What it must print |
|---|---|
| `sdl` | the SDL-built schema (given) |
| `code` | the schema built with `GraphQLObjectType`, `GraphQLEnumType`, `GraphQLDirective` builders and a `GraphQLCodeRegistry`: **exactly** the same text as `sdl` |
| `query sdl\|code '<query>'` | the same JSON from both: the same fetchers serve both schemas |
| `public` | the code-built schema without fields marked `@internal`, via `SchemaTransformer` |
| `bench 2000` | `sdl 2000 types <ms> ms`, `builder ...`, `fast ...`: SDL, `GraphQLSchema.newSchema()` and `GraphQLSchema.FastBuilder` |

```
bin/course java TwoWays.java code | diff - <(bin/course java TwoWays.java sdl)
```

## notes.md

Your three timings and four questions about when to use which.

## Done when

`bin/course test m05/07` is green and `bin/course grade m05/07` gives 7/10 or more.

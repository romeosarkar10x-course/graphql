# Rubric, lab 2.5 notes.md (10 points)

| Criterion | Points | Full marks |
|---|---|---|
| One vs two documents | 2 | A real benefit of one constant document (persisting, caching, codegen) and a real case for two |
| Spec rule | 3 | States that @skip/@include are forbidden on subscription root fields, and why: exactly one root field, decided without variables |
| Implementation gap | 3 | Reports graphql-java 26.1 accepting it, then the NullPointerException with a variable; draws the spec-vs-implementation lesson |
| Unknown directive | 2 | Exact UnknownDirective error; explains executable directives must be declared in the schema and implemented on the server |

Pass: 7/10.

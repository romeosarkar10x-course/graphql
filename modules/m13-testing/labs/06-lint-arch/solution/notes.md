# Lab 13.6 notes

## 1. Who caught what

| Violation | ArchUnit (mvn test) | Spotless | graphql-eslint |
|---|---|---|---|
| V1 controller reads a repository | caught | - | - |
| V2 service depends on graphql-java | caught | - | - |
| V3 tab and trailing spaces | - | caught | - |
| V4 `page_count` in the schema | - | - | caught (naming-convention) |
| V5 `@deprecated` with no reason | - | - | caught (require-deprecation-reason) |

Each tool sees one layer and nothing else. The compiler caught none of the five:
the code still compiles, the schema still loads, Spring serves `page_count` as null, and a deprecation
without a reason is valid SDL.

## 2. Why the layered rule alone missed V1

`layeredArchitecture()` allows graph to use store, because controllers return the store's records. So V1
passed the layered rule; it is the narrower `controllersNeverTouchRepositories` (no dependency on a class
annotated `@Repository`) that fails. Rules have to say what is banned, not just which packages may meet.

## 3. Formatter choice

I kept Spotless to hygiene steps (unused imports, trailing whitespace, tabs, final newline) so it does not
reformat the code base on day one. Switching to palantir-java-format or google-java-format later means one
big `spotless:apply` commit and then no more style discussion in reviews; a team should do that once and
put the commit in `.git-blame-ignore-revs`.

## 4. graphql-eslint's defaults against this schema

`flat/schema-recommended` reported 66 problems on a schema that already had a description on everything:
64 for one-line descriptions (it wants block `"""` descriptions) and 2 for payload types without an `id`.
Lint rules are a house style, not truth: I set `description-style: inline` and exempted `*Payload` and
`*Error` from `strict-id-in-types`, then added `require-description` and `require-deprecation-reason`.

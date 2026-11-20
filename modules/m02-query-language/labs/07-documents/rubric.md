# Rubric, lab 2.7 (10 points)

| Criterion | Points | Full marks |
|---|---|---|
| Operation hygiene | 3 | Names say what screen or job the operation serves; descriptions are useful; no operation fetches fields its screen does not need |
| Multiple operations | 2 | Exact error without operationName; explains the lone-anonymous-operation rule |
| Operation descriptions | 2 | Reports the parser error and that the September 2025 grammar allows it (spec ahead of graphql-java 26.1) |
| Extensions | 2 | Ignored when unknown, must be an object (HTTP 400); names a real use such as persisted query hashes or client identification |
| Why a folder | 1 | Two real benefits: per-operation observability, CI validation, codegen, persisted queries |

Pass: 7/10.

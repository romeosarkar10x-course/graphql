# Lab 12.1: A minimal client

Lecture: [12.1 What a client really does](../../lectures/12.1-what-a-client-does.md). Grading: auto + rubric.
Needs the reference server (`bin/course ref up`).

Write `ShelfieClient.java`: a GraphQL client on `java.net.http` and Jackson 3, no GraphQL library. The
starter has the signatures; `tests/Check.java` calls exactly those.

| Method | Must |
|---|---|
| `execute(document, variables)` | POST `{"query", "variables"}` as JSON with `Accept: application/graphql-response+json`; return status, `data` and `errors` (partial data included) without throwing |
| `executeNamed(name, variables)` | send `documents/<name>.graphql` with `operationName` = `name` |
| `field(name, variables, path, type)` | decode `data` at a dotted path (`"club.owner"`) into a Java type; null field gives `null`; any error is a `GraphQlException` carrying the errors **and** the partial data |
| `executeAsync(document, variables)` | identical requests already in flight share one HTTP call; the same variables in another key order count as identical; a later call sends again |
| (all) | a non-2xx status is a `GraphQlException` with that status, whether the body is a GraphQL response or an HTML error page; a dead server is one too (status 0) |

```bash
bin/course start m12/01
cd workspace/m12-clients/01-minimal-client
bin/course test m12/01        # runs tests/Check.java next to your ShelfieClient.java
bin/course jshell             # or try it by hand: /open ShelfieClient.java
```

The checks run your client against a stub server that records every request (to see your wire format and
to return errors, 400s and 502s on demand), then against Shelfie, where `/_course/stats` counts requests.

In `notes.md`: list what you had to write by hand, with rough sizes, and what a real library would still
add. That list is the case for the next three labs.

## Done when

`bin/course test m12/01` passes (17 checks) and `notes.md` passes the rubric (`bin/course grade m12/01`).

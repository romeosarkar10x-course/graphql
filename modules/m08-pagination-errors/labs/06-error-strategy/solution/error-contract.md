# Shelfie error contract

Written for client developers. Every statement here should be true of your server; the test checks
the codes, the grader reads the rest.

## Codes

| `extensions.code` | Meaning | Example | Retry? | What the client does |
|---|---|---|---|---|
| UNAUTHENTICATED | No viewer, or the credentials are bad | `myClubs` without `X-Shelfie-User` | After signing in | Send the user to sign in, then repeat the request |
| FORBIDDEN | Signed in, and this viewer may not do this | `deleteReview` on someone else's review | No | Hide or disable the action; show the message |
| NOT_FOUND | No such object, *or one this viewer may not know exists* | `joinClub("c99")`, and `joinClub("c3")` for a non-member of the private club | No | Treat as gone: remove it from the screen, or show "not found" |
| CONFLICT | The request contradicts current state | `joinClub` twice | No, not as is | Refetch the state; usually the goal is already reached (already a member) |
| INVALID_REQUEST | The request itself is wrong: syntax, validation, variables, argument rules | an unknown field, a missing `$id` | No | A bug in the client: log it, show a generic failure, fix the code |
| INTERNAL | Our fault: a bug or an outage | `Book.coverUrl` while object storage is down | Yes, with backoff | Show a generic failure and keep any partial `data`; quote `errorId` to support |

## Messages

- Messages are for people, not for code: show them, never parse them. Switch on `extensions.code`.
  `extensions.classification` carries the same value for clients that read it, and graphql-java's
  own classifications (`ValidationError`, `InvalidSyntax`) stay there for request errors.
- Domain errors (every code except INTERNAL) have a message written for the end user, safe to show:
  "Sign in first", "Not your review", "No club c99".
- INTERNAL errors always say `Internal error`. The exception, its message and the stack trace go to
  the server log only. Our own outage message names an S3 bucket, an object key and the address
  `10.0.4.17:9000`. None of that reaches a client.

## Partial data

A field error nulls that field (or its nearest nullable ancestor, lecture 8.4), and the rest of `data`
is real. `book { title coverUrl }` returns the title with `coverUrl: null` and one INTERNAL error at
`["book", "coverUrl"]`. Clients render what they got and use `errors[].path` to mark what is missing,
instead of failing the whole screen. `data: null` with errors means a non-null root field failed:
`myClubs` is `[Club!]!`, so UNAUTHENTICATED there leaves nothing. A response **without** a `data` key
is a request error: nothing ran.

## HTTP status codes (measured)

| Response | `Accept: application/json` | `Accept: application/graphql-response+json` | GraphQL over HTTP (September 2026) says |
|---|---|---|---|
| data, no errors | 200 | 200 | 200 |
| data and a field error (`coverUrl`) | 200 | 200 | 294 (SHOULD) |
| `data: null` and an error (`myClubs` signed out) | 200 | 200 | 294 (SHOULD), "even if it is null" |
| validation error (no data) | 200 | 400 | 4xx MUST; 422 SHOULD |
| syntax error (no data) | 200 | 400 | 4xx MUST; 400 SHOULD |

Measured on this server (Spring for GraphQL 2.1.0) on 2026-11-21 (`_workbench/2026-11-21-m08-probe/errors.txt`).
With `application/json`, everything is 200, as the spec requires for that media type. With
`application/graphql-response+json`, Spring returns 400 for every request error and 200 for every
executed request. That meets the MUST rules, but not the newer SHOULDs (422 for validation, 294 for
partial success). The status is chosen in `GraphQlHttpHandler.selectResponseStatus`, a protected
method. A `WebGraphQlInterceptor` can change headers but not the status, so getting 422 and 294 means
subclassing the handler. This server doesn't. Clients must therefore read the body, never the status
code, to decide what happened, which is what the spec tells them anyway. This server meets every MUST.


## Request ids and support

Every response has `extensions.requestId`. A client may send `X-Request-Id` (letters, digits, `-`, at
most 64 characters) to choose it, for example to match its own logs. Anything else is replaced with a
UUID, so a client can't inject text into our logs. An INTERNAL error's `extensions.errorId` equals the
`requestId`, and the server logs `errorId=<id> path=<path>` with the full exception, so a support
ticket that quotes the id finds the stack trace.

## Changing this contract

Codes are API. New codes may be added (clients must treat an unknown code like INTERNAL), but a code
is never renamed, reused for another meaning, or split without a deprecation period. Changing which
code a field returns, for example FORBIDDEN to NOT_FOUND, is a breaking change and gets the same
review as a schema change (lab 7.6). Messages may change at any time, which is why clients must not
parse them.

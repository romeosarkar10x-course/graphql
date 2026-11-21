# Lab 8.5 notes

Write predictions.json before you run Depths.java. Then run it and come back here.

## Q1. Which predictions were wrong? For each one: what did you expect, what happened, and which rule explains it?

(Reference: the key. A learner's answer names their own misses; the common ones are below.)

- **E3**, the usual surprise: `data` is `{"club": null}`, not a members list with a hole. `User.name` is
  `String!`, so u3's error nulls u3. The list is `[User!]!`: a null item is not allowed, so the list is
  null, but `members` is non-null too, so `club` is null, which it may be. Every `!` on the way up
  passes the null on to the parent.
- **E2/E6**: `owner: User!` takes the whole club with it. In E6 only alias `a` dies. The error path uses
  the response key `a`, not the field name `club`, and `b` is untouched, because propagation stops at
  the nearest nullable position, and siblings are never affected.
- **E1**: `data` is `null`. `hello: String!` is a root field, and the only parent left is `data` itself.
  One flaky greeting takes down the whole response, including the healthy `club`.
- **R3**: graphql-java classifies a missing variable as a `ValidationError` (spec: "variable
  coercion"). It is a request error, so there is no `data` key. People often predict a field error
  on `club`.

## Q2. R1, R2 and R3 have no `data` key at all, while E1 has `"data": null`. What is the difference, and what should a client do with each?

No `data` key means execution never started. The request was rejected by parsing (R1), validation
(R2) or variable coercion (R3). The spec says `data` must not be present then. Retrying the same
request gets the same error; it is a bug in the client, so log it, show a generic failure, and fix
the code. `"data": null` means execution ran and a non-null error reached the root. The request was
fine and the server failed while running it, so a retry may succeed, and the errors' paths say which
field. Over HTTP with `application/graphql-response+json` the status code also tells them apart
(GraphQL over HTTP, September 2026 edition): 4xx for a request error, `294` whenever `data` is
present together with `errors`, even `"data": null` (lecture 8.5).

## Q3. E3 and E7 both throw inside a list item. Why does one wipe out the whole club and the other leave a hole in a list?

The item types differ. `members: [User!]!` says items are never null, so a failed item can't be
nulled in place. The list goes, then `members` (non-null), then `club`. `reviews: [Review]` allows null
items, so review r2 becomes `null` at index 1 and r1 and r3 survive. The error path
`["club", "reviews", 1, "body"]` tells the client which hole it is. The schema author chose the blast
radius when writing `!`. That is the trade-off from lab 7.4: `[Review]` costs every client a null
check, and `[User!]!` costs the whole club page on any one bad profile.

## Q4. Look at the messages. What does a client learn about your servers from them? (Lecture 8.5 fixes this.)

graphql-java's default handler puts the exception message in the response: "users service down",
"ratings service down", "profile u3 unreadable", "review r2 corrupt". A client, or an attacker,
learns the internal service layout, which ids are broken, and the wording of internal exceptions. A
real `SQLException` would leak table and constraint names the same way. The classification
`DataFetchingException` is also an implementation detail, not a contract a client can switch on.
Lecture 8.5 masks the message, logs the details with a correlation id, and gives clients stable
`extensions.code` values instead.

# Errors without the blast radius: onError and service capabilities

Today, when a non-null field fails, GraphQL replaces the nearest nullable parent with `null` and bubbles the
error up. One failing `averageRating: Float!` on a book can wipe out the whole book, or the whole list it sits
in. Schema designers learned to defend against this by making fields nullable "just in case", so clients check
for `null` everywhere, even where `null` never means anything.

[Error behaviors (#1163)](https://rfcs.graphql.org/rfcs/1163/), at RFC 1 (Proposal), lets the client choose.
A request gains an `onError` property: `PROPAGATE` is today's behaviour and stays the default, `NULL` writes
`null` at the failing field only and keeps its siblings, and `HALT` stops at the first error, for scripts that
throw away any response with errors anyway. With `NULL`, a `!` can finally mean "never legitimately null"; the
client reads the `errors` list and decides, for example with an error boundary. A client-side helper can
re-create propagation where an app wants it.

A client cannot send `onError` blindly to a server that ignores it, which is where
[service capabilities (#1208)](https://rfcs.graphql.org/rfcs/1208/) comes in. Also at RFC 1, it was split out of
#1163 because it is useful on its own: a new `__service` meta-field lists capabilities such as
`graphql.onError`, each with a reverse-domain name, an optional description and an optional value. SDL gets a
matching `service { capability ... }` block. Tools could then configure themselves from the endpoint: does it
take operation descriptions, fragment arguments, `@defer`, which WebSocket URL?

What it means for Shelfie: not much yet. graphql-java 26.1 has neither `onError` nor `__service`, only an
experimental directive, `@experimental_disableErrorPropagation`, that switches propagation off for one operation.
But the direction is clear enough to design for: prefer honest non-null fields over defensive nullability where the domain says
so, keep clients tolerant of partial data, and plan to advertise features rather than document them in a wiki.

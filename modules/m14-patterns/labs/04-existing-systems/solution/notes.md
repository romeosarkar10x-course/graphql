# Lab 14.4 notes

## One page, three systems

The page is one query; behind it the catalogue answers over REST (`/api/books/{id}`), reviews and their authors over
gRPC with stubs generated from the service's own `.proto`, and Open Library over its public REST API. Each source
has one resolver, so each fails alone: the GraphQL response is partial by design, and the schema says which field
may go missing (`openLibrary` is nullable; `title` is not). A gRPC `NOT_FOUND` and a REST 404 are answers (null), not
failures. The REST payload carries a long synopsis the page never asked for: the BFF reads it, the client does not.

## The bad days

Measured in the grader: with Open Library hanging for 5 s the page answered in under 1.5 s (the 800 ms read
timeout), a 200 HTML maintenance page and a 503 both became `openLibrary: null` with `UPSTREAM_UNAVAILABLE`, and
with Open Library down for twenty pages the breaker let through at most a handful of calls, then stopped calling.
Three seconds after it recovered, a half-open trial call succeeded and the field came back. Without the timeout
every page waits for the slowest dependency; without the breaker every page pays the timeout and keeps hammering
a service that is already down.

## What a BFF owns

Timeouts, breakers and the mapping from upstream failures to GraphQL errors belong in the BFF, close to the
schema, because only it knows which field may be null. It also owns politeness: a User-Agent that names the
caller, and (mini-project 14.10) caching and batching so one page is one upstream call. SOAP would be one more
client class behind the same kind of resolver; the schema would not change.

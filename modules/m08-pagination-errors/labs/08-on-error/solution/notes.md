# Lab 8.8 notes

## Q1. The three modes on Page.QUERY: what does each return, how many fetchers ran, and how many fields did the cache keep?

| Mode | `data` | errors | fetchers | fields cached |
|---|---|---|---|---|
| PROPAGATE | `null` | 2 (rating, members[2].name) | 9 | 0 |
| NULL | the whole club; `rating: null`, `members[2].name: null` | the same 2 | 9 | 14 (6 records) |
| HALT | `null` | 1 (rating, the first one) | 3 | 0 |

(Reference run 2026-11-21, `_workbench/2026-11-21-m08-probe/onerror-lab.txt`.) PROPAGATE's two errors
each travel up to the root: `rating` is non-null, so `currentBook` (nullable) would absorb it, but
u3's `name` nulls u3, which `[User!]!` can't hold, then `members`, `club` and `club: Club!` at the root:
`data: null`. Nine fetchers did the work and the client gets nothing to cache. NULL does the same work
and keeps all of it except the two failed fields, and the error paths say exactly where those are.

## Q2. With NULL, `rating: Float!` came back as `null`. Is the schema lying? What must a client do so it never reads that null as a value?

No. Under NULL the `!` means "never null *unless there is an error at this path*". That is the
semantic non-null of lecture 7.4, applied to every `!`. The schema can now say what is true of the
data (a book always has a rating), instead of marking `rating` nullable just because the service can fail.
The price moves to the client: before reading a field, it has to check `errors` for that path and
throw or show an error state instead of the null. That is what graphql-toe and Relay's `@throwOnFieldError`
do. My cache does the same at write time: it skips errored paths, so an outage can't overwrite a
good cached rating with `null`. A client that reads `data` and ignores `errors` will show "no rating"
when the truth is "rating unknown", which is the risk.

## Q3. Which kind of client wants HALT, and what does the server gain from it?

Clients that throw the response away on any error anyway: scripts, batch jobs, CLI tools, some
public-API integrations (the RFC cites Shopify's Admin API users). For them partial data is noise,
and "it either worked or it didn't" is simpler to code. The server gains the work it no longer does:
my HALT run stopped after 3 of 9 fetchers, because cancelling the execution stops every fetcher not yet
started. On a real page those are database calls and downstream requests that a client would discard.

## Q4. Your prototype is built on an experimental directive. What would have to be true before you turned NULL on for a production API?

- The RFC settles. #1163 is RFC 1 (Proposal, last updated 2026-10-05), and the mode names have already
  changed once (ABORT became HALT). The working group decided on 2026-08-06 that PROPAGATE stays the
  default forever, so NULL must be opt-in per request, never a server-wide switch.
- The server supports the real `onError` request property, not my AST trick on
  `@experimental_disableErrorPropagation`, which graphql-java may rename or remove. Spring for GraphQL
  would also need to pass the property through.
- Every client that opts in handles errors per field (Q2). The opt-in should live in the client's
  trusted documents (the RFC's suggestion), so it is reviewed, not typed ad hoc.
- Tests like this lab's run against the server's own version, so an engine upgrade can't silently
  change the behaviour.

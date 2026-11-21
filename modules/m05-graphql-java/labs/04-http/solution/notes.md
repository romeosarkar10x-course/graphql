# Lab 5.4 notes

## Audit, before and after

| | Checks passing |
|---|---|
| Starter server | 6/25 |
| Your server | 25/25 |

## Questions

1. A07: the starter answers `Accept: text/html` with 200 JSON; a browser navigating to the endpoint gets
   a JSON blob instead of a clear 406, and a proxy cannot tell the client asked for something else.
   A15: validation failures return 200, so gateways, logs and alerting count a broken client release as
   success; with 422 an error-rate dashboard shows the spike. A24: the starter runs a mutation from a GET
   URL (it does not even read GET, but a naive GET handler would): any `<img src>` on another site could
   create a review as the logged-in user, which is a CSRF hole, and caches or prefetchers may replay it.
2. `294` is a custom code; it only means "partial success" when the media type says this is a
   GraphQL-over-HTTP response. A legacy client that asked for `application/json` may treat anything it
   does not know as an error or not cache it; RFC 9110 maps unknown 2xx codes to 200 anyway, so sending
   200 to it loses nothing and avoids breaking old code paths. The status code is for intermediaries
   (observability); clients read the body.
3. Whether a well-formed GraphQL response can be produced. An unparseable JSON body is not a GraphQL
   request at all, so the answer is an HTTP error that must not claim `application/graphql-response+json`.
   A document syntax error happens inside GraphQL processing: graphql-java produces a proper result with
   `errors` and no `data`, so it goes out in the GraphQL media type with 400.
4. Yes: the spec says a server MUST support POST and MAY support GET. Turning GET off loses
   CDN and browser caching keyed on the URL (persisted queries over GET, Module 9.6) and easy links for
   debugging. It gains a smaller attack surface: no mutation-over-GET checks, no long-URL limits (414),
   and no query text leaking into access logs.

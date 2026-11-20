# Lab 0.4 notes (reference)

## 1. The three status codes
- **GET = 405 Method Not Allowed.** Spring for GraphQL does not serve GET. The GraphQL-over-HTTP spec
  says a server MUST accept POST and MAY accept GET, so 405 is compliant. (Module 9 uses GET for CDN
  caching and has to enable it.)
- **text/plain = 415 Unsupported Media Type.** The spec requires `application/json` request bodies;
  a server may reject other media types. Rejecting `text/plain` also matters for security: browsers
  send it cross-origin without a CORS preflight (CSRF, Module 10.3).
- **Empty body = 400 Bad Request.** There is no GraphQL request at all; it is malformed before
  GraphQL is involved.

## 2. The 200 that was an error
`Accept: application/json` gives **200** with an `errors` array; `application/graphql-response+json`
gives **400**. With the legacy media type, intermediaries (proxies, caches, monitoring) cannot tell a
failed request from a good one; the new media type lets the status code mean something while keeping
old clients, which treat any 200 as "parse the body", working.

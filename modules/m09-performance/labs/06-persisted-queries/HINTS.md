# Hints, lab 9.6

1. `PersistedQueryCache.getPersistedQueryDocumentAsync(id, input, onMiss)`: on a hit, return the cached
   `PreparsedDocumentEntry`; on a miss, if `input.getQuery()` is empty or equals
   `PersistedQuerySupport.PERSISTED_QUERY_MARKER`, throw `PersistedQueryNotFound`; otherwise
   `onMiss.apply(query)` checks the hash, parses and validates, and you store what it returns.
2. On a hit, the input may still carry text. If it does, hash it yourself (SHA-256, lower-case hex) and
   throw `PersistedQueryIdInvalid` when it doesn't equal the id.
3. Spring sends no `Cache-Control` at all, and Varnish's built-in rules then keep any GET for 120 s. Set
   the header in a `WebGraphQlInterceptor` with `response.getResponseHeaders()`; a GET request is one
   whose URI has `query` or `extensions` parameters (`request.getUri().getQueryParams()`).

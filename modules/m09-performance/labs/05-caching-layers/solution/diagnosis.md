# Diagnosis

For each scenario, the guilty layer (one of: request, entity, response, http), then what was wrong and
how you fixed it. Keep the `N: layer` lines exactly in this form; the test reads them.

1: request
2: entity
3: response
4: response
5: http

## 1

The books loader is per request (BatchLoaderRegistry creates it for each request) and memoises by id.
The first addReview loaded book 11 into it; renameBook saved the new title in the database and the
entity cache, but the loader still held the old Book object, so `after.book` came from the loader's
memo. Fix: `loader.clear(id).prime(id, renamed)` in renameBook. Clue: it happens inside one request
only, with no cache header involved.

## 2

The Caffeine BookCache kept book 32 for ten minutes after the rename: save() wrote to the database and
never invalidated the entry. A different document (so no response-cache hit, X-Response-Cache: MISS) still
showed the old title, and a restart fixed it: an in-JVM cache. Fix: invalidate the id in save().

## 3

The Redis response cache key was the document, operation and variables, but not the viewer, so
`{ viewer { handle } }` for reader 6 was a HIT on reader 5's entry. Fix: add X-Shelfie-User to the key.
Same-key personal data is the classic response-cache leak.

## 4

Same document twice: the second response was X-Response-Cache: HIT with zero SQL statements. Nothing
dropped cached responses when a mutation changed data. Fix: invalidateAll() after renameBook and
addReview (coarse, but correct; tag keys by book id to invalidate less).

## 5

The GET responses said `Cache-Control: public, max-age=60` even with X-Shelfie-User, so Varnish stored
reader 7's response and served it to reader 8 (X-Cache: HIT). Fix: `private, no-store` for anything
personal or with errors, public only for anonymous responses, and `Vary: X-Shelfie-User`.

# Hints, lab 9.5

1. Sort the evidence first. Inside one request, with no cache header involved: the per-request layer.
   Wrong across requests, `X-Response-Cache: MISS`, but statements > 0: something in this JVM kept an
   old object. `X-Response-Cache: HIT` and 0 statements: Redis answered. `X-Cache: HIT`: Varnish did, and
   the app never saw the request.
2. A cache is stale for one of two reasons: nobody tells it the data changed (invalidation), or two
   different questions share one key (the key misses something that changes the answer). Ask both
   questions of every layer that holds a book title or a viewer.
3. Fixes that keep the caches: the mutation tells each layer about the change (`DataLoader.clear(id).prime(id, value)`,
   `Cache.invalidate(id)`, deleting the cached responses); the response key includes everything the
   response depends on; and the HTTP layer says `private, no-store` for anything personal.

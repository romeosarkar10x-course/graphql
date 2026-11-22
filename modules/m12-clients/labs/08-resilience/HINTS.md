# Hints: m12/08

1. Find out what a failed call throws before you configure anything: point the client at a port nobody listens
   on and print the exception class. Retry only that kind of failure (and 5xx), never GraphQL errors or
   userErrors, which are answers.
2. Order matters: `Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(breaker, call))` makes every
   attempt count in the breaker; tell the retry to ignore `CallNotPermittedException`, or an open breaker is
   retried with backoff too. Choose a failure-rate threshold above the weather (about 35% here).
3. For `createReview`, a transport failure means "unknown". Retry, remember that an earlier attempt was
   unknown, and when the server then says `DUPLICATE`, the earlier attempt worked: query `user(id:) { reviews {
   id book { id } } }` for its id.

# Hints: m14/06

1. Start with the logs: run the starter, make two requests, and grep its output for an email address. A log line
   is copied to more places, and kept longer, than any table; log ids, never names or text. Then read what the
   audit stores for `createReview`.
2. `Instrumentation.instrumentExecutionResult` sees every execution once, after it ran: parse
   `parameters.getQuery()`, pick the operation (`parameters.getOperation()` or the only one), and for each top-level
   `Field` of a mutation record `getName()`, the argument names, and whether an error's path starts with its
   result key. The viewer is in `parameters.getGraphQLContext()`.
3. An erase is one transaction over every table that names the person, then the caches. The audit log's actor is
   a link to the person: rewrite it. After the erase the actor of the `deleteMyAccount` entry no longer exists,
   which is a fine way to know what to write.

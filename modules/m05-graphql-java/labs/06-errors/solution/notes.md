# Lab 5.6 notes

## Five exceptions, five errors

| Exception | classification | message the client sees | extensions | Field error or `userErrors`? |
|---|---|---|---|---|
| NotFoundException | NOT_FOUND | No such club zz | — | Usually neither: a nullable field returning null is the GraphQL way for "no such thing" on reads; a payload error on writes |
| ForbiddenException | FORBIDDEN | Not allowed to read ... | — | Field error: the client did nothing wrong in the data, and the rest of the response can still render |
| InvalidInputException | BAD_USER_INPUT | Rating must be 1 to 5, got 9 | field: rating | `userErrors` in a mutation payload: the UI shows it next to the input |
| TimeoutException | UNAVAILABLE | A service Shelfie depends on is not answering | retryable: true | Field error: transient, not part of the domain; the client may retry |
| NullPointerException | INTERNAL | Internal error (ref 7ff5e267) | ref | Field error, always masked: nothing the client can act on except reporting the ref |

## Questions

1. See the last column. The rule I used: if a client is expected to handle it as part of the product
   (validation, conflicts), it belongs in the schema as data; if it is exceptional or infrastructural
   (outage, bug, permission at the transport level), it is a field error.
2. "Exception while fetching data (/e) : Cannot invoke "String.length()" because "<local3>" is null",
   and the TIMEOUT row leaked "ratings-service did not answer in 2000 ms" (an internal service name and
   its timeout). Any exception message goes out verbatim: a bug one line later would have sent the JDBC
   URL with an internal IP. An attacker learns the stack, the database, internal hosts and which inputs crash the
   server, and can probe for more.
3. `mustWork` failed, so its value is null; a null in a `String!` position is itself a field error, so
   the spec says null propagates to the parent field: `club` becomes null (it is nullable, so it stops
   there; `hello` is unaffected). Two errors were raised but only one per failed field is reported, here
   the TIMEOUT for `mustWork` and the FORBIDDEN for `trouble`. Worth it when the parent is meaningless
   without the field (an id, a price); a nullable field is kinder when the rest of the object is still
   useful (a club page without its rating).
4. Search the logs for `3f9c0a1b`: the handler logged the ref with the path and the full exception (and,
   in production, the trace id and stack trace). From the path, find the fetcher; from the exception, the
   line. The client never needed the details to report the problem.

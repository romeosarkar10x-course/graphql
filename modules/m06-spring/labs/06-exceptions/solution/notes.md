# Lab 6.6 notes

## Error snapshot

```
a NOT_FOUND       "No such club zz"
b FORBIDDEN       "Not allowed to read this club's notes"
c BAD_REQUEST     "Rating must be 1 to 5, got 9"                 extensions.field = rating
d BAD_REQUEST     "The club was changed by someone else"        extensions.code = CONFLICT, currentVersion = 7
e INTERNAL_ERROR  "A service Shelfie depends on is not answering" extensions.retryable = true
f INTERNAL_ERROR  "INTERNAL_ERROR for <execution id>"            (Spring's default, the details in the log)
```

## Questions

1. Every one became `"INTERNAL_ERROR for bba88465-..."` with classification `INTERNAL_ERROR`
   (`_workbench/2026-11-21-m05-probe/spring-default-errors.txt`). graphql-java's default sent the exception
   message, internals and all. Spring's is safer: it leaks nothing, but it also tells the client nothing
   it could act on, so domain errors must be mapped deliberately. The id is the execution id, the same for
   every error of the request, which support can search for in the log.
2. CONFLICT is a `BAD_REQUEST` plus `extensions.code`, and TIMEOUT an `INTERNAL_ERROR` plus `retryable`.
   Five generic types keep clients simple, but an own `ErrorClassification` enum (as in lab 5.6) puts the
   distinction where clients look first and documents the set in one place; I would use one for a public
   API and keep `code` stable either way.
3. When the exception and its meaning are specific to that controller's fields (a `CoverTooLarge` only the
   upload controller throws), or to override the global mapping for one controller. Shared domain
   exceptions belong in the advice so the mapping is consistent everywhere.
4. INVALID and CONFLICT: in a mutation they are expected outcomes the UI must handle, so they belong in
   the payload (`userErrors`, or a result union with a `Conflict` member carrying `currentVersion`), typed
   in the schema. NOT_FOUND on a query field is usually better as `null`.

# Hints, lab 5.4

1. Decide the order first: method, then `Accept` (can we answer at all?), then the body (`Content-Type`,
   JSON, shape), then GraphQL. Each step has its own status code; once you return, stop.
2. After `toSpecification()`, the status follows from the map: no `data` key means a request error (400
   if the document does not parse, 422 otherwise); `data` with `errors` is a partial success; `data`
   alone is 200. Re-parse the document to tell the two request-error cases apart.
3. For GET, decode the URL's query component, JSON-decode `variables` and `extensions`, and find the
   operation that `operationName` picks (`graphql.parser.Parser` and `OperationDefinition`) before
   executing: if it is a mutation, answer 405 with an `Allow` header and run nothing.

# Hints: 18.3 Agent safety

1. Do the cheap rules first: delete the `userId` parameters and read the header instead
   (`RequestContextHolder` works, because synchronous MCP tools run on the servlet request's thread), and make the
   write tools a separate `@Component` with `@ConditionalOnProperty`. Half the checks pass after that.
2. Guard `run_graphql` with graphql-java itself, which Spring for GraphQL already puts on the class path: `Parser`
   for the document, `OperationDefinition.getOperation()` for its kind, `Validator` against Shelfie's SDL
   (`/graphql/schema`), `QueryTraverser` for depth and introspection fields, `QueryComplexityCalculator` with
   Module 10's `FieldComplexityCalculator` for the cost. Price variables at their defaults.
3. An approval is a small state machine keyed by an unguessable id: pending, approved or denied by the human,
   done after one confirm. Bind it to the reader who asked, give it an expiry, and check all four on confirm. The
   human's routes are ordinary `@RestController` methods that compare the token in constant time
   (`MessageDigest.isEqual`).

# Lab 5.2 notes

1. Your tree has more lines than fetchers you wrote. Which lines come from fetchers nobody wrote, and
   what does graphql-java do in them?
2. Read the order of your tree. Does graphql-java resolve the query depth-first, breadth-first, or
   something else? Point at two lines that prove it.
3. `roleInClub` could have been computed in other ways: an extra argument, a field on a wrapper record,
   the GraphQLContext. Why is local context the right carrier here, and when would GraphQLContext be?
4. `booksById` returns a `DataFetcherResult` with data and errors. Write the JSON a client gets for
   `["b2", "zz"]`, and say what a client should do with it.

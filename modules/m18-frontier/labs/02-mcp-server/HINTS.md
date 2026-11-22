# Hints: 18.2 An MCP server in Java

1. Start with one tool, `list_clubs`, end to end: a `@Tool` method that calls
   `shelfie.documentName("ListClubs").executeSync()` and returns `getData()`. Run the test; the error it prints
   for the other seven tells you what is left.
2. If Shelfie answers `InvalidSyntax` for every operation, it is reading the descriptions: `HttpSyncGraphQlClient`
   has a `documentSource(...)` builder method, and a `DocumentSource` only has to return a `Mono<String>` for a
   name. Remove the strings that come right before `query`, `mutation`, `fragment` or a `$variable:`.
3. Optional parameters: `@ToolParam(required = false)` with a boxed type (`Integer`, `Boolean`), and put a
   variable into the map only when the agent sent it, so the operation's own default (`$first: Int = 5`) applies.

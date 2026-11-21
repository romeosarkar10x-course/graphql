# Hints, lab 5.1

1. `ExecutionResult` has a method that returns a `Map<String, Object>` laid out exactly as the spec's
   Response section describes. Jackson can write that map as it is.
2. Run `run` on the 5,000-field query and read the error: it names a limit. Limits like that live in
   `graphql.parser.ParserOptions`; `ParserEnvironment` lets you pass options to one parse call without
   changing the defaults for everyone (the `run` test checks that you did not).
3. To execute a document you already parsed, graphql-java asks a `PreparsedDocumentProvider` for it. The
   provider is handed a function that would parse and validate; you do not have to call it.

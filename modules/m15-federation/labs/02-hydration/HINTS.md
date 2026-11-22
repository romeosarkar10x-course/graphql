# Hints: m15/02

1. `ServiceExecutionParameters` holds the query as a `graphql.language.Document`; graphql-java's `AstPrinter`
   turns it back into text. The result is a `NadelServiceExecutionResultImpl(data, errors, extensions)`, all three
   as plain maps and lists, the way the service's JSON parses.
2. The directive's arguments name the catalog field to call (`field`), how to fill its argument from the review
   (`arguments: [{ name: ..., value: "$source.<underlying field>" }]`) and, for a batch field, how to match
   results back to reviews (`identifiedBy`). The source field must exist in the **underlying** schema, not in the
   overall one.
3. One `book(id:)` call per review is hydration without batching: it works and makes N calls. Point the hydration
   at the list field instead. If Nadel complains at start-up, it is comparing your `.nadel` files with
   `underlying/`: the message names the type and field that do not match.

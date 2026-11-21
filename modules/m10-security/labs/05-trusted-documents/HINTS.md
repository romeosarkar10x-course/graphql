# Hints, lab 10.5

1. The id is `sha256(body)` in lowercase hex, where body is exactly what the client sends: the file's
   text with leading and trailing whitespace removed. Jackson 3 (`tools.jackson.databind.json.JsonMapper`)
   is on `bin/course java`'s class path for writing the manifest.
2. `getDocumentAsync(input, parseAndValidate)` decides everything: return
   `parseAndValidate.apply(input.transform(b -> b.query(body)))` to run a trusted body, or a
   `PreparsedDocumentEntry` built from a `GraphQLError` to refuse. The hash is in
   `input.getExtensions().get("persistedQuery")`.
3. Check the cases in this order: an id in the request, or else the hash of the text; then "is the id in the
   manifest?"; then "if there is text, does it hash to that id?". Only then fall back to development mode.

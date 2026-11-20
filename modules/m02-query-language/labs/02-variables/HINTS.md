# Hints, lab 2.2

1. Start in GraphiQL: write `query BookSearch($text: String!, $first: Int = 5) { books(titleContains: $text, first: $first) { ... } }` and put `{"text": "the"}` in the Variables pane. When it works there, it works from Java.
2. The request body is JSON with up to three keys: `query`, `operationName` and `variables`. `variables` is a JSON object, so a `Map<String, Object>` that Jackson serialises is all you need. Read the document with `Files.readString(Path.of("book-search.graphql"))`.
3. `Map.of(...)` cannot hold an optional key cleanly: build a `HashMap`, always `put("text", ...)`, and `put("first", Integer.parseInt(args[1]))` only when there is a second argument. The document must contain no string literals at all.

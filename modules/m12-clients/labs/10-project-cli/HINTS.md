# Hints: m12/10

1. Load the schema once at startup with an introspection query that asks, per type, for `fields { name args {
   name type } type }` and `possibleTypes { name }`; unwrap `NON_NULL` and `LIST` (`ofType`) to get each field's
   named type. Completion is then "which type am I inside, and am I in a field list, an argument list, or after
   `... on`?".
2. Scan the text before the cursor once, left to right, skipping strings and comments. Keep a stack: on `{`
   push the type of the field just before it (the root type at the top: `query`, `mutation`, or a bare `{`),
   on `}` pop. A `(` after a field means its arguments until the matching `)`; inside them, `{` and `[` are
   values, not selection sets.
3. The JLine side is small: a class that `implements org.jline.reader.Completer`, adding one `Candidate` per
   name to the list, given to `LineReaderBuilder.completer(...)`. Lines typed so far for an unfinished document
   are part of the context: keep them in a buffer the completer can see, and run the document when its braces
   balance.

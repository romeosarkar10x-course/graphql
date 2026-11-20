# Hints, lab 2.1

1. Every field that returns an object needs its own `{ ... }`: `owner { ... }`, `currentBook { ... }`. In GraphiQL, Ctrl+Space inside the braces lists what you can pick.
2. Jackson maps a JSON object onto a record by matching keys to component names, so the record for `owner` needs components called exactly `name` and `handle`. A record nests inside another record the way the selection sets nest.
3. `currentBook` is nullable: check `c.currentBook() == null` before reading it. `authors` is a JSON array of strings, so `List<String>` works, and `String.join(", ", list)` gives the format the card wants.

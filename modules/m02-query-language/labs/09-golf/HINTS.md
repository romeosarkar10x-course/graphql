# Hints, GraphQL golf

1. Commas are whitespace in GraphQL, and whitespace is needed only between two names: `{clubs{name}}` is a complete document. The `query` keyword is optional for an anonymous query.
2. An alias renames a key: `n:memberCount`. String arguments need their quotes; ID arguments accept a string.
3. A union field needs `... on Type` for every field except `__typename`. You only need a type condition for the types that actually appear in the target.

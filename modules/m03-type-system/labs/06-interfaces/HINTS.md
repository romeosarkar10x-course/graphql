# Hints, lab 3.6

1. `.type("Node", t -> t.typeResolver(env -> env.getSchema().getObjectType("Book")))` is the shape. `env.getObject()` is the Java value the field returned.
2. `switch (env.getObject()) { case Data.Book b -> "Book"; ... }` with a `default` that throws: a value with no GraphQL type is a server bug, and silence would hide it. `Reviewable` needs a resolver even though only `Book` implements it, and so does `Node` even if no field returns `Node` directly.
3. `recent` mixes two record types in one list: `Stream.concat(...)` them as `Object`, sort by a `createdAt` helper that handles both, newest first. ISO-8601 strings in UTC sort correctly as strings.

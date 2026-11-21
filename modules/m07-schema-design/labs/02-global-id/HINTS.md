# Hints, lab 7.2

1. `Base64.getUrlEncoder().withoutPadding()` and `Base64.getUrlDecoder()`; the decoder throws
   `IllegalArgumentException` on bad input, which is your "empty". Split on the **first** `:` with
   `indexOf`, not `split(":")`, so keys keep theirs.
2. A record `Kind<T>(Class<T> javaType, Function<String, Optional<T>> load, Function<T, String> key)`
   in a `Map<String, Kind<?>>` keyed by GraphQL type name gives you all three directions: name to loader,
   object to name (`javaType().isInstance(o)`), object to key.
3. The `id` fetcher must be wired on each object type (`Club.id`, `User.id`, ...), because the default
   property fetcher would return the record's raw `id()`. The type resolver is wired on the interface:
   `.type("Node", t -> t.typeResolver(env -> env.getSchema().getObjectType(nameOf(env.getObject()))))`.

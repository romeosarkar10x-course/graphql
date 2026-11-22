# Hints: m15/03

1. Compose after every change and read only the first error: composition stops at the first stage that fails,
   and one missing `@key` can surface as an error about field sharing or interfaces. Each subgraph must import
   every directive it uses in its own `@link`.
2. Two subgraphs may both define a type's fields in two ways only: the type is an entity (same `@key` in both;
   each adds its own fields) or the field is `@shareable` in all of them. A field one subgraph only *mentions*
   so that it can use it (`handle` for `@provides`, `pages` for `@requires`) is `@external` there.
3. `@interfaceObject` lives in the subgraph that does *not* define the interface: it declares an object type with
   the interface's name and key. The interface itself needs `@key` in catalog, and then every implementation
   needs the same key. `@override` goes on the new owner's field, and the old owner keeps declaring it.

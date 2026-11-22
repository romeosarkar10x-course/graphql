# Hints: 17.6 introspection and printing

1. The introspection types are ordinary object types: `resolve` is a switch on the parent type and field name.
   A `__Type` is a `TypeRef` around an `Ast.Type`: `NonNullType` is kind `NON_NULL` with `ofType`, `ListType` is
   `LIST`, a `NamedType` is looked up in the schema.
2. Fields with `includeDeprecated` filter on the `@deprecated` directive; `deprecationReason` is its `reason`
   argument or the default. `defaultValue` is the value printed as GraphQL (`Printer.value`), not JSON.
3. For printing, follow the order of definition, print a description on one line as a string and on several lines as a block string, skip the built-ins and the
   introspection types, and print a `schema { ... }` only when the root types are not named `Query`, `Mutation`,
   `Subscription`. The stable check prints your own output again: any trailing space or reordering shows.

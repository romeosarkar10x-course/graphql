# Lab 3.9 — The full Shelfie schema, split five ways

**Lecture:** [3.9 Directives, extensions, deprecation, coordinates](../../lectures/3.9-extensions-coordinates.md) · **Grading:** auto + rubric · **Needs:** nothing

This is Module 3's checkpoint schema: everything Shelfie has so far, modelled with what Module 3 taught,
split by area so that each team (clubs, books, users, feed) owns a file.

## Do

1. Split `schema/shelfie.graphqls` into **at least five** files under `schema/` (they merge in name
   order). One base file defines `schema`, scalars, shared interfaces and a minimal `Query`; each area
   file defines its types and **extends** `Query` (and `Mutation`, if you add one) with its own fields.
2. Grow it to **15+ named types**, using what this module covered: custom scalars with
   `@specifiedBy`, `Node`, an enum or two, a union, a `@oneOf` input, a mutation input and payload.
3. Deprecate one of each: a **field**, an **argument**, an **input field** and an **enum value**,
   always with a `reason` that says what to use instead. (Required arguments and input fields cannot be
   deprecated: try it.)
4. `lint.json`: 10+ rules, each naming its target by **schema coordinate** (`Type`, `Type.field`,
   `Type.field(arg:)`, `Enum.VALUE`, `Input.field`, `@directive(arg:)`), at least 4 kinds. Check them:
   `../../../bin/course gql coordinates schema <coordinate>...`
5. Write `directive-on-directive.graphqls` from lecture 3.9 and run
   `../../../bin/course gql schema-lines directive-on-directive.graphqls`. Record what happens.
6. `notes.md`.

Useful: `../../../bin/course gql schema-lines schema` (the merged schema) and `../../../bin/course gql stats schema`.

## Done when

`bin/course test m03/09` is green and `bin/course grade m03/09` gives 7/10 or more.

# Records-to-SDL: design notes (reference)

## Nullability

Nullable by default fits GraphQL: the spec makes every type nullable unless marked, and a field that
can fail should be nullable (lab 3.4); non-null is a promise you opt into per field. Non-null by default
(Kotlin's style) reads better in Java code but turns every forgotten annotation into a schema promise
that one failing resolver can break, blanking whole screens.

## What code-first lost

Arguments (`members(first: Int = 10)`), deprecations, `@oneOf` inputs, custom scalars with
`@specifiedBy`, `extend type` across files, root operation types. Arguments: methods with parameters
on the record (or an annotated resolver class), each parameter becoming an argument, with an
`@DefaultValue` annotation.

## What code-first won

Lab 3.1 experiment 3 (`memberCount` renamed in SDL only, wiring unchanged, silently null) and the
unwired-field class of bug: the field *is* the record component, so they cannot drift. Lab 3.7's
missing union member: the union is the sealed interface's permitted list.

## Question

Either, argued. Schema-first: the SDL is the reviewed contract, designed before code, readable by
non-Java clients; Module 6's Spring for GraphQL and DGS are schema-first. Code-first: one source of
truth, refactoring tools rename both. The other camp's reply should be stated fairly.

# Lab 3.8 notes (reference; outcomes in table.txt)

## The 14 lookups

| Case | Outcome | Stage | Spec (September 2025, Example 86) |
|---|---|---|---|
| 01-03 | book | - | valid |
| 04-unknown-id | `book: null` | - | valid input; nothing found |
| 05-empty `{}` | rejected | validation | "Exactly one key must be specified" |
| 06-two-keys | rejected | validation | same |
| 07 `{id: "b1", title: null}` | rejected | validation | same: a null still counts as a key |
| 08 `{id: null}` | rejected, `book: null` with a field error | execution | "Value for member field must be non-null": argument coercion runs during execution |
| 09 variable, two keys | rejected, no data | variable coercion | same rule, raised while coercing variables |
| 10 variable `{id: null}` | rejected, no data | variable coercion | "must be non-null" |
| 11 variable, one key | book | - | valid |
| 12 `{id: $i}`, `$i: ID` unset | rejected, field error | execution | spec: "Value for member field must be specified"; graphql-java's message says "must be non-null" |
| 13 `{id: $i}`, `$i: ID!` | book | - | valid |
| 14 unknown key | rejected | validation | not a oneOf rule: input fields must exist |

## Two that surprised you

08: a literal null is valid *syntax* for a nullable field, so validation passes; the oneOf rule is a
coercion rule, and argument coercion happens when the field executes. 12: the document is valid for
any `$i`, so only at execution, when `$i` turns out absent, does the lookup have zero keys.

## Inhabitability

`OneOf Input Object A must be inhabited but all fields recursively reference only other OneOf Input
Objects forming an unresolvable cycle.` A oneOf value must set exactly one field to a non-null value,
so every A contains another A, forever. `input B { b: B }` allows `{}` or `{b: null}`: a finite value.
(RFC #1211 added this rule; graphql-java 26.1 enforces it.)

## Question

One field: one entry point to document, cache and authorise; adding a fourth key is additive. Three
fields: each argument can be `String!` with its own description and deprecation, no `@oneOf` support
needed in old clients or tools; but three resolvers and three cache keys for one thing.

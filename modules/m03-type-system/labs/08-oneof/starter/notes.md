# Lab 3.8 notes

## The 14 lookups

| Case | Outcome | Stage | Does it match the spec's Example 86 table? |
|---|---|---|---|
| 01-id | | | |
| 02-isbn | | | |
| 03-title | | | |
| 04-unknown-id | | | |
| 05-empty | | | |
| 06-two-keys | | | |
| 07-two-keys-one-null | | | |
| 08-null-key | | | |
| 09-var-two-keys | | | |
| 10-var-null-key | | | |
| 11-var-ok | | | |
| 12-inner-nullable-var-unset | | | |
| 13-inner-non-null-var | | | |
| 14-unknown-key | | | |

## Two that surprised you

Pick two rows whose stage surprised you and explain why they fail where they do.

## Inhabitability

Paste graphql-java's message for `cycle.graphqls`. In your own words: why is `input A @oneOf { a: A }`
impossible to use while `input B { b: B }` (nullable, not oneOf) is fine?

## Question

`book(by: BookLookup!)` or three fields `bookById`, `bookByIsbn`, `bookByTitle`? One reason each.

# Lab 3.1 notes (reference)

## Experiment 1: delete one wiring entry

Without the `currentBook` fetcher: `"currentBook": null`, **no error at all**. The default
PropertyDataFetcher finds no `currentBook` component on `Data.Club` (it has `currentBookId`) and
returns null; `currentBook: Book` is nullable, so null is a legal answer. The bug is silent.

## Experiment 2: delete another

Without the `owner` fetcher: `"club": null` and one error, classification
`NullValueInNonNullableField`, path `[club, owner]`: "The field at path '/club/owner' was declared as
a non null type, but the code involved in retrieving data has wrongly returned a null value."
`owner: User!` cannot be null, so the null propagates to the nearest nullable parent, `club`.
Loud, and it takes the whole club with it (lecture 3.4).

## Experiment 3: break the contract

The schema still builds: graphql-java does not check that wiring names match fields (a fetcher for a
field that does not exist is just never called). `membersCount` falls to the default fetcher, finds
nothing and returns null; it is `Int!`, so `club` is null with an error. A test that runs a real query
(this lab's tests), or the audit, catches it; `SchemaGenerator` does not. Code-first (lab 3.13) makes
this class of mismatch a compile error instead.

## Question

False alarm: a field resolved by a type-wide default fetcher, a `DataFetcherFactory`, or a getter
method on a class (the audit only knows record components). Miss: a fetcher that is registered but
returns the wrong thing, or a record component of the right name with the wrong meaning (an `id` that
is a database key, not the public id).

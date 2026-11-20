# Lab 2.1 notes (reference)

## Experiment 1: remove `handle` from the query, keep it in the record

What the card printed: `owner    Kemi Adeyemi (@null)`

Why: the response mirrors the query, so `handle` is simply absent from the JSON. Jackson leaves a record
component it cannot find at its default, and for a `String` that is null. No error anywhere: the bug
shows up in the UI.

## Experiment 2: remove `memberCount` from the query, keep it in the record

What happened: `MismatchedInputException: Cannot map null into type int (set
DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES to 'false' to allow)`.

Why is this different from experiment 1? An `int` cannot be null, and Jackson 3 turns
FAIL_ON_NULL_FOR_PRIMITIVES on by default, so a missing primitive fails loudly while a missing object
stays silent. If the field may legitimately be absent, use `Integer`; if it must be there, failing is
the better behaviour.

## Experiment 3: add `description` to the query but not to the record

What happened: nothing; the card is unchanged. Jackson 3 ignores unknown properties by default
(FAIL_ON_UNKNOWN_PROPERTIES is off). Good for evolution (the server can add fields), bad for typos: a
`String` component misspelled as `handel` silently stays null. Generated types (Module 13) remove the
problem.

## The envelope

- `data`: the result of executing the operation. Absent when the request failed before execution
  (syntax or validation error), null or partial when a field error bubbled up.
- `errors`: present only when something went wrong; can appear together with `data`.
- `extensions`: optional map for implementation-specific extras such as tracing or query cost.

# Lab 3.5 notes (reference)

## The v1 server and ABANDONED

`Can't serialize value (/shelf[1]/status) : Invalid input for enum 'ReadingStatus'. Unknown value
'ABANDONED'`. `status: ReadingStatus!`, entries `[ShelfEntry!]!`, and `shelf` is non-null at the root,
so null bubbles all the way: `data: null`. Loud is right: the server must never emit a value its own
schema does not declare; the fix is to ship the schema (v2) before the data.

## Strict vs tolerant client

OldClient: `InvalidFormatException: Cannot deserialize value of type OldClient$ReadingStatus from
String "ABANDONED"`. The shelf screen fails to load for every u3-like user, not just the one row.
TolerantClient shows `UNKNOWN` (render as "Other", hide, or prompt to update). It loses the meaning:
it cannot act on ABANDONED, and code that switches on status must have an UNKNOWN branch.

## Question 1

Safe for the server and for clients that only send values. It breaks clients that receive the enum
and decode it into a closed type: Java/Kotlin/Swift enums with default decoders, exhaustive `switch`.
GraphQL-aware codegen (Apollo Kotlin, Swift) generates an unknown case for this reason.

## Question 2

The schema is a public contract and outlives storage. Names are self-documenting in GraphiQL,
introspection and generated code; codes leak a storage detail clients would then depend on, so a
migration (P/X to a boolean column) would break them. The provider keeps the mapping in one place.

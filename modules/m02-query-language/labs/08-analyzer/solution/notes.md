# Lab 2.8 notes (reference)

## Flat vs fragments

Fragments are a way of writing the document, not a different request: after expansion both are the
same tree, so they cost the server the same. An analyzer that counts text (characters, lines, fields
as written) undercounts fragment-heavy documents; it has to expand spreads first, as graphql-java's
`MaxQueryComplexityInstrumentation` does (Module 10).

## The scary ones

Measured against the reference server (JSON objects in the response, 2026-11-21):

- `07-deep`: depth 9 but only 9 fields as text; the response holds 106 objects, because every level
  fans out (members, their reviews, each book's last 5 reviews). Depth catches it; field count as
  written does not.
- `08-alias-fanout`: depth 4 and 25 fields, yet 216 objects: five aliases repeat the same
  3 clubs × members × reviews. Alias count (or field count after expansion) catches it; depth does not.

## A limit you would set

The deepest of the 12 operations is ClubPage at depth 5 (club > currentBook > reviews > author > name);
the largest is ClubPage with 20 fields and CompareClubs has the most aliases (2). Allowing depth 6,
60 fields and 4 aliases leaves headroom for real
screens and rejects both scary fixtures by at least one metric. Better still: persisted operations for
first-party clients and a cost budget for third parties (Module 10).

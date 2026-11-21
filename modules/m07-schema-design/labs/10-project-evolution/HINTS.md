# Hints, mini-project 7

1. Every request has the same shape of answer: a **new field** with a good name next to the old one,
   and `@deprecated(reason: "Use <new field>: <why>.")` on the old one in the same step. The type of an
   existing field never changes.
2. Request 2 needs data about a pair (member and club): that's a type of its own, as in lab 7.3. Request
   5 returns two unrelated types in one list: lecture 7.5 says which abstract type fits when members share
   nothing.
3. Request 6 is the only step allowed to break anything, and only by removing exactly what the usage
   logs say is unused. Read the release-1 clients in `clients/v1/` before you delete: one deprecated field
   is still selected.

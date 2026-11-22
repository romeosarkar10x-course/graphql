# Lab 14.1 notes

## Stacking: does order matter?

Yes. graphql-java calls each directive's `onField` while it builds the schema, in the order the directives are
written on the field, and each wiring wraps the data fetcher it finds. So the first directive written is the one
nearest the resolver and runs first on the value. `currencyThenMask` turns 12.50 EUR into "GBP 10.62" and then
masks it to "*******62"; `maskThenCurrency` masks "12.50" to "***50" and the conversion gets a string that is no
amount, so the field is null with an `@currency` error and the rest of the response survives. The spec says the
same in "Directive Order Is Significant" (2.13): two orders may mean different things. Uppercase and mask
commute, which is why stacking them looks harmless.

## Directive or code?

The directive puts the rule where every client and reviewer reads it: `email: String! @mask` is visible in the
SDL and in introspection, the resolver stays "return the email", and a new PII field needs one word, not a
resolver change. The wiring validates its use when the schema is built, so `@uppercase` on a Float stops the
server at start-up, naming `Book.priceEur`; the same rule written in resolvers would fail on the first request,
or never. The cost: behaviour is now in two places (SDL and the wiring), the order rule is invisible unless you
know it, and a directive cannot see who is asking unless it reads the context.

## Where a directive is the wrong tool

Authorisation that depends on the viewer and the object (`@auth(role:)` is fine for coarse rules, but "only the
author" needs data), anything that changes types (masking an Int as a String must be a String field), and
business rules that need other data (`@currency` with live rates becomes a network call hidden in the schema).

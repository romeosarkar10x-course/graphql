# Lab 2.5 notes (reference)

## One document, two cards

One constant document means one thing to persist, cache and allowlist, one generated type, and the
UI flips a variable. Two documents are better when the variants diverge so much that the shared part
is small, or when you want separate metrics per variant.

## The subscription root

`gql validate` said: `valid`.

The spec's "Single Root Field" rule forbids @skip and @include in a subscription's root selection set:
a subscription must have exactly one root field, and the server has to determine it during validation,
without runtime variables. graphql-java 26.1 does not enforce this part of the rule.

With a variable: the validator crashes with `NullPointerException ... ConditionalNodes.getIfValue`
because it tries to evaluate the directive without variables. The reference server turns that into an
HTTP 500. The spec says invalid; the implementation says valid or crashes. That gap is why Module 17
reads the spec rather than trusting one implementation.

## Directives you cannot use

`Validation error (UnknownDirective@[hello]) : Unknown directive 'uppercase'`. Executable directives
must be declared in the schema (`directive @uppercase on FIELD`) and implemented by the server, for
example by wrapping the field's data fetcher (Module 14).

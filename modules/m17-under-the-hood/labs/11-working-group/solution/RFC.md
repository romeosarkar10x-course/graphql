# RFC: Float literals without a leading digit

Champion: reference solution
Stage asked for: 1 (proposal)

## Problem

`{ book(id: 1) { price(discount: .5) } }` is a syntax error. Users write `.5` because many languages accept it, and
the error does not say why. Our engine rejects it, as the grammar says: every `FloatValue` alternative starts with
`IntegerPart` (2.10.2). A client can write
`0.5`, so this is a convenience, not a capability.

## Proposed spec edits

In 2.10.2 Float Value and C.1:

```
FloatValue ::
  - IntegerPart FractionalPart ExponentPart [lookahead != {Digit, `.`, NameStart}]
  - IntegerPart FractionalPart [lookahead != {Digit, `.`, NameStart}]
  - IntegerPart ExponentPart [lookahead != {Digit, `.`, NameStart}]
  - NegativeSign? FractionalPart ExponentPart? [lookahead != {Digit, `.`, NameStart}]
```

And a note: "`.5` and `-.5` are Float values." The `Punctuator` `...` stays: `...` is lexed first, as today, and
`..5` stays an error.

## Engine change

In `Lexer.java`, the `.` branch: when the next character is a digit (and the token is not `...`), lex a number
with an empty integer part; `-` followed by `.` and a digit likewise. Test: `{ f(x: .5, y: -.5e1) }` gives two
`FloatValue` tokens; `{ ...F }` and `{ f(x: ..5) }` behave as today.

## Compatibility

No valid document changes meaning: every document that has `.5` today is invalid. But every other parser rejects
it until it implements the RFC, so a client that sends `.5` breaks against older servers: the classic problem of a
grammar extension. Printers keep printing `0.5`.

## Alternatives

A better error message ("Did you mean `0.5`?"), which needs no spec change and gets most of the value: the
recommended first step.

## The meeting

- Implementer: "The lexer's `.` branch now has three outcomes (`...`, a number, an error). Cheap." Agreed.
- Client author: "Clients that send `.5` would only work on new servers. Tooling (codegen, persisted documents)
  would have to normalise." Not answered well: there is no way to negotiate grammar versions.
- Spec editor: "Guiding principles: favour no change. What does this enable that `0.5` does not?" Nothing.

Outcome: **rejected** at strawman, with the alternative recorded: better error messages in implementations.

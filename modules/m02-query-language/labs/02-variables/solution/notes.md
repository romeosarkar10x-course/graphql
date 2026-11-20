# Lab 2.2 notes (reference)

## What the starter sent

The document the starter built from the payload:

```graphql
{ books(titleContains: "", first: 20) { publishedYear title } __schema { types { name } } } #", first: 5) { publishedYear title } }
```

## What the server did with it

It returned all 10 books (the empty string is contained in every title, and the payload replaced the
limit of 5 with 20) plus the full list of types. The server sees one well-formed document; it cannot
tell which characters came from the developer and which from the user. The `#` turned the developer's
real ending into a comment. `say "hi"` instead produced a syntax error: the same bug, less dangerous.

## Two other things an attacker could put in that string

- A deeply nested or aliased selection to make the server do expensive work (Module 10).
- Fields the screen never asks for, such as other users' data, if an authorization check is missing.

## Why variables fix it

The document is a constant written by the developer; the values travel separately as JSON and are
coerced against the declared types (`String!`). A quote in a variable is just a character in a string,
never syntax. Constant documents also make persisted queries and allowlists possible (Module 9.6, 10).

## When you leave `first` out of the variables map, who decides the value?

The operation's variable default (`$first: Int = 5`). If the variable had no default, the field
argument's schema default (`first: Int = 20`) would apply. Sending `"first": null` explicitly is
different again: it passes null.

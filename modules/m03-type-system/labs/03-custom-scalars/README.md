# Lab 3.3 — An ISBN scalar, both input paths

**Lecture:** [3.3 Custom scalars](../../lectures/3.3-custom-scalars.md) · **Grading:** auto + rubric · **Needs:** nothing

A `Coercing` has three jobs: `serialize` (Java to response), `parseValue` (a variable's JSON value to
Java) and `parseLiteral` (a value written in the document to Java). The starter's draft does all three
differently. You make them agree, and prove it with a table.

## The ISBN rules

- Input must be a **string**. Hyphens and ASCII spaces are ignored; nothing else is.
- 13 ASCII digits starting `978` or `979`, with a valid ISBN-13 check digit; **or** 10 characters,
  9 ASCII digits then a digit or `X`/`x`, with a valid ISBN-10 check digit, converted to ISBN-13.
- The Java value and the output are always the 13 digits, no hyphens.
- `serialize` rejects anything that is not a valid ISBN string: a bad ISBN in your data is a server bug.

## Do

1. Start the lab and run `../../../bin/course java Isbn.java table ../../../modules/m03-type-system/labs/03-custom-scalars/tests/bad-inputs.json`.
   The starter does not build yet: TODO 2 first, then look at the table. Keep a copy for `notes.md`.
2. **TODO 1:** `normalise` and the three `Coercing` methods, to the rules above. Throw
   `CoercingParseLiteralException`, `CoercingParseValueException` or `CoercingSerializeException`.
3. **TODO 2:** register `ExtendedScalars.DateTime`, `GraphQLBigDecimal` and `Url`, and wire
   `Book.coverUrl`, `Book.listPrice` and `Review.createdAt`.
4. Add `@specifiedBy(url: ...)` to `scalar ISBN`.
5. `notes.md`.

The table prints, for each input, what happens when it is a literal (`{ check(isbn: "...") }`) and when
it is a variable (`query($i: ISBN!) { check(isbn: $i) }`): `ok <value>`, or `rejected/<stage>`.

## Done when

`bin/course test m03/03` is green and `bin/course grade m03/03` gives 7/10 or more.

# Lab 4.5 — Where does each rule belong?

**Lecture:** [4.5 Input validation](../../lectures/4.5-input-validation.md) · **Grading:** auto + rubric · **Needs:** nothing

A bad input can be stopped in four places. The starter stops everything in the resolver, by hand.
Move each rule to where it belongs, and watch where 20 invalid inputs (and one valid one) are rejected.

| Layer | Who checks | Client sees | Rules for it |
|---|---|---|---|
| **type** | graphql-java itself (validation, variable coercion) | a request error, no `data` | types, required fields, unknown fields |
| **directive** | graphql-java-extended-validation, from the SDL | a field error with `ExtendedValidationError` | shape: ranges, lengths, patterns, list sizes |
| **bean** | Jakarta Bean Validation (Hibernate Validator 8) on the input record | `userErrors`, code `INVALID` | rules that need Java: not blank, no repeated tags |
| **business** | your resolver | `userErrors`, codes like `NOT_FOUND` | facts about the world: the book exists, u1 has not reviewed it |

## The rules

`bookId` looks like `b` and digits; `rating` 1 to 5; `body` 10 to 2000 characters and not blank; at
most 5 `tags`, each at most 20 characters, no repeats; the book must exist; u1 may review a book once
(u1 already reviewed b1, b2 and b8).

## Do

1. Run `../../../bin/course java Validation.java ../../../modules/m04-mutations/labs/05-validation/tests/cases.jsonl`
   on the starter. Every rejection says `bean` (the starter reports everything as `INVALID` user errors).
2. Shape rules → directives in `schema.graphqls` (`@Range`, `@Size`, `@Pattern`, `@ContainerSize`; declare
   them, see the lecture) and a `ValidationSchemaWiring` in `graphQL()`.
3. Java-only rules → Bean Validation annotations on `CreateReviewInput` (`@NotBlank`, an `@AssertTrue`
   method), validated in the resolver; violations become `INVALID` user errors with the property as field.
4. The resolver keeps the business rules.
5. `notes.md`.

The output format (`<case> <layer> <detail>`) comes from `layer()`; do not change it.

## Done when

`bin/course test m04/05` is green and `bin/course grade m04/05` gives 7/10 or more.

# Lab 3.3 notes

## The starter's table

Paste the starter's table for the 15 bad inputs (after TODO 2, before TODO 1). Mark every row where the
literal and the variable column disagree. For three of them, say which `Coercing` method caused it.

## Your table

Both columns now reject every bad input. Why does the literal column say `validation` and the variable
column say `variable-coercion`? What does that mean for a client sending a bad ISBN?

## Question 1

Why must `serialize` throw on an invalid ISBN rather than pass it through?

## Question 2

`ISBN` as a custom scalar, or `isbn: String` plus a validation rule? Give one cost of each.

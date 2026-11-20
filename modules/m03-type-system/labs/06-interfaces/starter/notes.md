# Lab 3.6 notes

## The startup error

Paste it. Which interfaces does it name? Why does graphql-java refuse to start rather than fail on
the first query?

## Experiment 1: forget one

With your solution working, remove the resolver for `Timestamped` only. Paste the error.

## Experiment 2: the transitive rule

Change `type Book implements Reviewable & Node` to `type Book implements Reviewable`. Paste the error.
Why does the spec make you repeat `Node`?

## Question

`Reviewable` has one implementing type. Was it worth an interface? Argue yes or no.

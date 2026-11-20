# Lab 4.1 notes

## Serial vs parallel

Paste your two `total` lines and the start/end times. What did the starter's query take, and why was
it not parallel even though graphql-java's query strategy is asynchronous?

## Why serial?

Give a concrete two-field mutation where running the fields in parallel would give a wrong answer.

## Selecting data back

Part 1 made four requests. How many would it take if each mutation returned only `Boolean`? What does
selecting data back give a client cache (Apollo, Relay, Module 12)?

## Question

The spec allows query fields to run in parallel. Name one thing a resolver author must therefore never
assume.

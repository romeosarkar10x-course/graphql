# Lab 4a notes

## What broke

Paste the starter's failure for one seed. For each broken invariant, which chaos mode (or plain
concurrency) caused it?

## Your fix

What is the unit of atomicity in your service, and why that one? What would you change to let
transfers between different users run in parallel?

## Remembering answers

How long must the service remember a requestId? What happens if it forgets too early, and what does it
cost to remember too long?

## Question

The harness trusts what clients were *told*. Why is "points conserved according to the answers" a
stronger property than "points conserved"?

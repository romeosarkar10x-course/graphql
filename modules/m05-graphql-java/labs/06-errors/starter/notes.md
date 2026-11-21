# Lab 5.6 notes

## Five exceptions, five errors

| Exception | classification | message the client sees | extensions | Field error or `userErrors`? |
|---|---|---|---|---|
| NotFoundException | | | | |
| ForbiddenException | | | | |
| InvalidInputException | | | | |
| TimeoutException | | | | |
| NullPointerException (a bug) | | | | |

## Questions

1. For each row, say in the last column whether this failure should really be a top-level error or a
   typed result in the payload (Module 4.3), and why.
2. What did the default handler leak for the bug, and how could an attacker use it?
3. `mustWork` is `String!`. Explain what happened to `club` in `nested.graphql`, step by step, citing the
   spec's rule for non-null fields. When is that worth it, and when is a nullable field kinder?
4. A support ticket quotes "Internal error (ref 3f9c0a1b)". Walk through how you find the cause.

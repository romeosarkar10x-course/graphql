# Lab 2.12 notes

## REST vs GraphQL for this card

How many REST calls would the same card take (https://docs.github.com/en/rest)? List them.

## What the request costs

Add `rateLimit { cost remaining resetAt }` next to `user` once, run it with your token, and record the
cost. Why does GitHub charge by query cost rather than by request?

## Errors

What does GitHub return for a login that does not exist? How did you tell that apart from a real failure?

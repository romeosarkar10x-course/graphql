# Lab 3.5 notes

## The v1 server and ABANDONED

Paste the error from the v1 shelf query for u3. Why is `data` null and not just one entry? Is a loud
failure the right behaviour here?

## Strict vs tolerant client

Paste OldClient's error. What would a user of the shipped app see? What does TolerantClient do
instead, and what does it lose?

## Question 1

The schema evolution rule says adding an enum value is "safe". Safe for whom? Name who it breaks.

## Question 2

Why expose `PUBLIC`/`PRIVATE` rather than `P`/`X`, when the codes are what is stored?

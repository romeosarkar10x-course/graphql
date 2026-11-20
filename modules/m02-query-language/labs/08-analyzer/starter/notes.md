# Lab 2.8 notes

## Flat vs fragments

`02-profile-flat` and `03-profile-fragments` measure the same. Why must they, and what does that say
about where an analyzer (or a cost limiter) has to look?

## The scary ones

Look at `07-deep` and `08-alias-fanout`. Each one is small as text. Estimate how many objects the
server resolves for each against Shelfie's seed data, and say which metric would have caught it.

## A limit you would set

If Shelfie were public, what maximum depth, field count and alias count would you allow? Justify one
number with the 12 operations from lab 2.7.

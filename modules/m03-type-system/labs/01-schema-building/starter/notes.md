# Lab 3.1 notes

## Experiment 1: delete one wiring entry

Remove the `currentBook` fetcher, run the club-page query for c1. What comes back? Any error?

## Experiment 2: delete another

Put `currentBook` back. Remove the `owner` fetcher instead. What comes back now? Why is this
different from experiment 1? (Look at the two field types.)

## Experiment 3: break the contract

Rename `memberCount` to `membersCount` in `schema.graphqls` only (keep the wiring). Does the schema
still build? What does the query return? What would have caught it?

## Question

The audit lists fields that will be null. Name one situation where it gives a false alarm and one
where it misses a real problem.

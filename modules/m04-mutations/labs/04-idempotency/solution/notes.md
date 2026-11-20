# Lab 4.4 notes (reference; runs in _workbench/2026-11-21-m04-probe/idempotency-*.txt)

## Before and after

Starter: 50, 48, 46 reviews (and some requests fail with a primary-key error on the key table). Fixed:
1, 1, 1. The count varies because it depends on scheduling: how many threads pass the SELECT before
the first INSERT commits.

## Why the database, not Java

A JVM lock or map works for one instance of one process. Two instances behind a load balancer, or a
restart between the attempt and the retry, and it is gone. The unique constraint holds across every
writer and survives restarts, and it is in the same transaction as the review, so "key claimed" and
"review written" cannot disagree.

## Optimistic vs pessimistic

Optimistic (version check): low contention, user-facing edits where a conflict can be shown ("someone
renamed it, reload"). Pessimistic (`FOR UPDATE`): high contention on one row where retries would storm,
or a short critical section that must not fail, e.g. decrementing stock or a balance.

## The job pattern

It retries and starts a second import. The mutation needs an idempotency key too (same pattern as
`createReview`, keyed to the job id), and a client-visible way to find its jobs (`jobs(status:)`).

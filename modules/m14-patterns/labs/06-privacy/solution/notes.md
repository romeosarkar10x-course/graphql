# Lab 14.6 notes

## The erase request, verified

After `deleteMyAccount` the grader looked in four places for Oluwaseun: the API (`user(u5)` null, even though it
had just been read and cached), every table in `m14_06` as text, the audit log, and the app's log output. Nothing
named them, nothing quoted their reviews ("Ikeja" was in one). The audit log kept its rows with the actor
rewritten to `erased`, so the history of what happened survives without saying who. Personal data hides in more
places than the users table: review text, caches, logs, audit trails, backups and Kafka topics, which this lab
does not cover (backups expire on their own schedule; topics need compaction with tombstones or crypto-shredding).

## What the audit keeps

One row per top-level mutation field, written by an Instrumentation after execution, so no resolver can forget
it: when, a user id, operation, field, argument names, outcome. Not argument values: a review body or a search
term is personal data, and audit logs live longer than anything else. The failed `Vandal` delete is audited
too; failed attempts are what an audit is for.

## Soft delete and retention

`deleted_at` hides a review from every read at once and lets its author undo it; a nightly job (and an admin
mutation) purges after 30 days, so "deleted" eventually means deleted. Every read path must filter it, which is
the cost: one forgotten `deleted_at IS NULL` resurrects a review. An erase is a hard delete on purpose: a
soft-deleted personal record is still a personal record.

# Audit notes

## Before and after

graphql-http's summary line (`{"total": ..., "ok": ..., "warn": ..., "notice": ..., "error": ...}`) for the starter
and for your server.

## What you changed

Each change, the audit check it fixed (its id), and the rule in the September 2026 edition it follows.

## What you left

Every warning and notice still in the report: the audit's id, what graphql-http wants, what the edition says (quote
it), and why you follow the edition.

## Batching and persisted documents

Why a batch is refused, and what a client should do instead. What the persisted-documents proposal changes in a
request, and what it would take to support it here.

# Client resilience: notes

## Before and after

<!-- Paste the test's plain, resilient and unreachable lines. Explain the plain number from the toxics: why
     ~30% lost responses plus 5% hangs gives about that success rate, and why Toxiproxy needs a fresh
     connection per call to hit that rate at all. -->

## What is safe to retry

<!-- Which of Shelfie's operations can your client retry blindly, which not, and how did your reviews command
     tell "the server stored it and the answer was lost" from "it failed"? What would you need from the server
     to make createReview safe to retry without the DUPLICATE trick (think idempotency keys)? -->

## The breaker

<!-- Your breaker's settings and why: what failure rate is weather and what is an outage? What does a retry
     without a breaker do to a server that is coming back up? -->

## Persisted documents from the client

<!-- Lecture 12.8: what a client sends with automatic persisted queries, what happens on the first call after
     a server restart, and why the retry logic must treat PersistedQueryNotFound differently from a transport
     failure. -->

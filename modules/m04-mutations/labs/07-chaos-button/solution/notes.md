# Game 4a notes (reference)

## What broke

Starter, seed 1: 321 requestIds answered differently on retry; balances summed to 12,243 while
answers added up to 8,884; tote stock 2 taken, 1 reported. Retries and duplicates double-applied
awards (lost responses and slow retries: the effect ran twice); concurrent read-modify-write lost or
duplicated balance changes and let stock and balances be decremented by two threads that both saw
enough. Calm (one client, no retries) hides all of it.

## Your fix

The whole mutation, under one service lock, including the requestId lookup. A transfer must change two
balances together and must see a balance no other request is changing; a single lock is the smallest
correct thing. For parallelism: lock the two users in a fixed order (by id) to avoid deadlock, keep the
answered map concurrent, and claim the requestId first (put a pending marker, other copies wait on it).

## Remembering answers

At least as long as any client may retry: the client's retry window plus clock and queue slack (Stripe
keeps idempotency keys 24 hours). Too short: a late retry re-applies the effect. Too long: memory or a
table that grows forever; expire by age, store in the database next to the effect (lab 4.4).

## Question

"Conserved" alone allows a server that applies an award and then tells the client it failed (or the
reverse): totals add up, and the user's screen is wrong. Tying the invariant to the answers means every
effect the server applied was reported, exactly once, and nothing reported failed was applied.

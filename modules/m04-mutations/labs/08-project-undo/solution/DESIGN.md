# Undo everything: design notes (reference)

## Your commands

Rename(clubId, from, to): conflict unless the name is still `to`. Join(clubId, user): conflict unless
still a member. Leave: conflict if a member again. Create(review): conflict unless the same review is
still there. Delete(review): conflict if a review with that id exists again. Inverses pair up:
rename↔rename, join↔leave, create↔delete.

## Expiry

Short, matching the UI: 10 to 30 seconds for an "Undo" toast, longer (days) for a history view. Store
commands in a table next to the data, written in the same transaction as the change (like lab 4.4's
idempotency keys), with an expiry column; a periodic job deletes old rows.

## Undo vs idempotency

No: a retried undo after a lost response gets `USED`, so the client believes it failed though it
worked. Fix: remember the undo's answer per token and replay it for the same caller (store the
response with the used token), exactly game 4a's requestId pattern.

## Question

Anything with effects outside Shelfie: sending email invites, a payment, publishing to followers. You
cannot unsend an email; such mutations need confirmation before, not undo after (or a delay before
the side effect happens).

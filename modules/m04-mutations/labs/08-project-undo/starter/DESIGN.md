# Undo everything: design notes

## Your commands

List each command and its conflict rule in one line.

## Expiry

The tokens never expire here. What expiry would you choose for a real app, and where would you store
commands so a server restart does not lose them?

## Undo vs idempotency

Is `undo` itself idempotent? What happens when the client retries an undo whose response was lost
(game 4a)? How would you fix that?

## Question

Name one Shelfie mutation that should not get an undo token at all, and why.

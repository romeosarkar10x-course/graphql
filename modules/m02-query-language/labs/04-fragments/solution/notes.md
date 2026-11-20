# Lab 2.4 notes (reference)

## Your fragments

- `UserCard` (id, name, handle, avatar): the avatar chip, used by the profile header, club owners and
  user search hits.
- `BookSummary`: the book tile, used by current books, reviews and book search hits.
- `ClubSummary`: the club row, used by the user's clubs and club search hits; it spreads `UserCard`.
- `ReviewItem`: the review list entry; it spreads `BookSummary`.

## Byte-identical

First attempt put `...UserCard` after `bio` and `location`, so `avatarUrl` moved below `location`. Same
data, different bytes: fields come back in the order they are first collected, and a spread contributes
its fields at the place where it is spread. Moving the spread to the top restored the original order.

Compare: `diff <(jq -c . old.json) <(jq -c . new.json)` for bytes (key order matters), and
`diff <(jq -S . old.json) <(jq -S . new.json)` for data only.

## The cycle

Validation happens before execution and without data. Expanding `ClubWithOwner` inside itself never
terminates as text, whatever the data would be, so the spec forbids cycles statically
(rule "Fragment spreads must not form cycles"). A client that wants three levels writes three levels.

## Fragment arguments (RFC #1224)

Today a fragment cannot take parameters, so the same tile with "last 3 reviews" and "last 10 reviews"
needs two fragments or operation-level variables shared by everyone. Fragment arguments let the spread
pass its own values. graphql-java 26.1's parser rejects the syntax:
`Invalid syntax with offending token '(' at line 1 column 7`.

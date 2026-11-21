# Lab 7.3 notes

## 1. Keys you removed

Each `*Id`/`*Ids` field of the starter, what replaced it, and what a client no longer has to do.

## 2. List or connection?

For `Club.members`, `User.clubs`, `Book.authors` and `Book.reviews` (not in the lab schema; decide anyway):
list or connection, and the reason (how big can it get? does the pair carry data? does a screen page it?).

## 3. Edge data

Why `role` and `joinedAt` live on `ClubMemberEdge`, and what goes wrong if you put `role` on `User`.

## 4. Experiment: the client update

The review card before and after: how many requests, how many lines, and what a client that still
sends `authorId` sees from your new schema (run `bin/course design clients` on the starter's file).

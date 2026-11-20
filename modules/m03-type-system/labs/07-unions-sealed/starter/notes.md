# Lab 3.7 notes

## The starter's bug

What did the starter return for the `BookFinished` item in c1's feed, and why no error?

## The compiler error

Add `record ClubRenamed(String clubId, String at, String newName) implements FeedItem {}` at the
marker line. Paste the compiler error. Then remove it.

## Question 1

Union or interface for the feed? Every item has `at`. Would `interface FeedItem { at: String! }` be
better? Give one argument each way.

## Question 2

Adding a member to a union is a schema change. Who can it break, and how should clients write their
queries so it does not?

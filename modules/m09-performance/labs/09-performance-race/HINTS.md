# Hints, game 9a

1. Count first. 1,000 of the 1,250 statements are `Review.book`, and 200 are `Reader.reviews`: the
   deepest fields. Fix those two and you're at 50 statements.
2. Every field with a fan-out below the root needs a batch. The ones with arguments need the argument
   in the loader key, and `ownerPicks` needs chained loaders to be dispatched (lab 9.3).
3. After 8 statements, the p95 is mostly work in the JVM. Lecture 9.4 measured whether one statement
   beats eight, and lab 9.7 measured what a document cache saves. The flame graph says what's left.

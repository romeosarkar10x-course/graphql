# Game 9b — Guess the query count

**Lecture:** [9.1 Seeing N+1](../../lectures/9.1-seeing-n-plus-one.md) · **Grading:** auto · **Needs:** postgres

Ten queries, ten guesses: how many SQL statements does each one run? Rounds 1 to 7 run on the naive
app from lab 9.1, rounds 8 to 10 on the batched app from lab 9.3. The data is `bin/course clubs` (100
clubs; every club has 25 members; every tenth club has no current book; each reader has 10 reviews).

The queries are in [`rounds.md`](starter/rounds.md). Guess from the schema and the resolvers alone. Running the
queries first spoils the game.

## Play (solo)

1. `bin/course start m09/10`. For each round, write the count in `guesses.txt`, one `N: count` per line.
2. `bin/course test m09/10` runs each query on both reference apps and scores you: 2 points for an exact
   guess, 1 within 10%, 0 otherwise. It prints exact, close or miss per round, never the count.
3. You pass with **14 of 20**. Then run the queries yourself with lab 9.1's counter and work out every
   miss. The misses are where the lesson is.

## Play (group, 3 or more players)

1. The instructor (or whoever finished lab 9.3) starts both apps and shares a screen.
2. For each round: show the query, 60 seconds of silence, everyone writes a number on paper, reveal
   together. Then run it and read `extensions.sql.statements` aloud.
3. Score as above; ties broken by the sum of distances on the rounds missed. Add the top three to
   `games/leaderboards/guess-the-query-count.md`.
4. After each round, the closest player explains their arithmetic in one sentence. Pause on the rounds
   nobody got: there is usually one.

## Done when

`bin/course test m09/10` scores 14 or more.

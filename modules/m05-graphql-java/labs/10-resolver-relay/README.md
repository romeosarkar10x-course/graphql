# Game 5a — Resolver relay

**Lecture:** [5.2 Data fetchers in depth](../../lectures/5.2-data-fetchers.md) · **Grading:** auto + rubric · **Needs:** nothing

A schema is a message from one person to another. This game measures how much of the message gets
through when the only channel is names and types.

## Solo variant

1. Read `relay.graphqls` (written by "player A": no descriptions). Do **not** open `INTENT.md` yet.
2. Implement every field in `Relay.java` from its name and type alone. Write what you implemented and
   why in round 1 of `guesses.md`.
3. Now read this lab's [INTENT.md](INTENT.md). Fill round 2: which fields matched, and the name, type
   or description that would have made the intent unmissable.
4. Fix `Relay.java` until `bin/course test m05/10` is green. The test lists every field that still
   differs from the intent.

Score: fields you matched in round 1, out of 8. The rubric grades round 2 and the lessons.

## Group variant (pairs)

1. Player A writes a small schema of their own over `Data.java` (6 to 8 fields beyond ids and names),
   with no descriptions, and an intent file kept secret.
2. Player B implements it in 20 minutes without asking anything.
3. Swap: B writes, A implements.
4. Reveal intents together, score each other, and rewrite both schemas with names and descriptions that
   would have scored 8/8. Then do the solo variant as the auto-graded part.

## Done when

`bin/course test m05/10` is green and `bin/course grade m05/10` gives 7/10 or more.

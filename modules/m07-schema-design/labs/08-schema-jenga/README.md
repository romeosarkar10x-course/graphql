# Game 7a — Schema Jenga

**Lecture:** [7.6 Designing for evolution](../../lectures/7.6-evolution.md) · **Grading:** auto + rubric · **Needs:** nothing

`tower/tower.graphqls` is the tower: lab 7.1's Shelfie schema. `tower/frozen/` holds six client operations
from app versions that are installed on phones and will never update. A schema change **pulls a block**:
if any frozen client stops validating, the tower falls.

Settle any change with the tools from this module, from the lab directory:

```
../../../bin/course design clients <proposed.graphqls> ../../../modules/m07-schema-design/labs/08-schema-jenga/tower/frozen/
../../../bin/course design diff ../../../modules/m07-schema-design/labs/08-schema-jenga/tower/tower.graphqls <proposed.graphqls>
```

`design clients` is the referee ("all 6 clients ok" = HOLDS). `design diff` is what a schema-diff
gate would say. They don't always agree, and that is the lesson.

## Solo: the deck

`tower/deck/01.graphqls` to `10.graphqls`: ten proposed changes, one per card (first line).

1. In `bets.md`, bet BREAKS or HOLDS on all ten **before running anything**. Read the frozen clients
   first; that is allowed.
2. Settle each card: fill Tower (BREAKS/HOLDS), Clients broken (the count) and SchemaDiff (the verdict).
3. Score one point per right bet. Answer "After the game".

## Group: build your own tower (3 to 6 players)

1. Copy `tower/` to a shared folder. Each round, the proposer writes a change to `turns/<name>-<round>.graphqls`
   with a one-line **product reason** ("clubs want a cover image").
2. Everyone else bets BREAKS or HOLDS, privately, in 30 seconds.
3. Settle with `design clients`. Bettors score 1 per right bet. The proposer scores 2 if the tower
   **holds** and the change delivers the reason, 0 if it falls. A falling change is reverted; a holding
   one stays, so the tower gets harder each round.
4. After a holding round, the proposer may add one new frozen client that uses the new feature: that's
   a new block others must not pull.
5. Play 2 rounds per player. Highest score wins. Debrief on the round where SchemaDiff and the tower
   disagreed.

## Done when

`bin/course test m07/08` is green (bets filled, columns settled) and `bin/course grade m07/08` gives 7/10 or more.

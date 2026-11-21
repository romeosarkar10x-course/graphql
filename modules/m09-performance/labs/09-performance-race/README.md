# Game 9a — Performance race

**Lecture:** [9.7 Measuring properly](../../lectures/9.7-measuring.md) · **Grading:** auto · **Needs:** postgres

One fixed query, the club dashboard (`contract/dashboard.graphql`), and the naive app from lab 9.1:
1,250 SQL statements per request. Make it fast. Anything goes inside the app: loaders, lookahead SQL,
indexes you create at startup, a document cache. Two things don't:

- the response must stay byte-for-byte the same data (`contract/dashboard.expected.json`);
- no response cache and no HTTP cache: every request must reach the database at least once. A cached
  answer is a different race (lecture 9.5).

## Play (solo)

`bin/course start m09/09`, then work in `workspace/m09-performance/09-performance-race/`. The judge
(`bin/course test m09/09`) loads fresh data and builds two apps: yours, and its own copy of the naive one as the
baseline. It sends each 50 warm-up dashboards, then 200 measured ones, one at a time, and prints:

```
statements per dashboard   8          (naive: 1250)
p95                        12.8 ms    (naive: 49.3 ms on this machine, now)
```

| You pass when | |
|---|---|
| statements per dashboard | 9 or fewer |
| p95 | at most half the naive app's p95, measured in the same run |
| data | identical to the expected dashboard |
| every request reaches the database | at least 1 statement per request |

The baseline runs on the same machine in the same test, so a slow laptop doesn't lose you the race.
Score: statements first (fewer wins), then p95.

## Play (group, 2 to 8 players)

1. 45 minutes, everyone on their own copy. No sharing code; sharing ideas out loud is allowed and
   encouraged in the last 15 minutes.
2. At the end, everyone runs `bin/course test m09/09` on the same machine (take turns, so runs don't
   compete for the CPU) and reads out both numbers.
3. Two boards in `games/leaderboards/performance-race.md`: lowest statements, and lowest p95 as a
   percentage of the naive baseline. The same person can top both.
4. Debrief: the winner of each board explains their biggest single change. Did the fewest statements
   also give the lowest p95? (Lecture 9.4's measurements say it might not.)

## Done when

`bin/course test m09/09` passes.

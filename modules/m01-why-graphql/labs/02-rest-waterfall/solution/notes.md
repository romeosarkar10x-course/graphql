# Lab 1.2 notes (reference, measured 2026-11-21)

## 1. Cost of the page, REST
| Club | Requests | Bytes | ms, no latency | ms, 700 ms latency |
|---|---|---|---|---|
| c1 | 11 | 4,917 | 75 | 2,182 |

11, not 13: a per-run user cache saves the owner (also member #1) and Chloe Martin (member and reviewer).

## 2. The dependency graph
```
club ──┬── owner, 5 members
       ├── book
       ├── reviews ── 3 reviewers
       └── meetups
```
Minimum 3 sequential round trips: club, then everything that needs only the club, then the reviewers.
At 700 ms each that is a 2.1 s floor, which the measurement matches (2,182 ms). A naive sequential
client pays 11 × 700 ms ≈ 7.7 s.

## 3. Bytes you threw away
The page JSON is about 700 bytes; 4,917 were downloaded, so about 85% was discarded (avatar URLs,
preferences, stats, synopsis, club rules...). That is over-fetching.

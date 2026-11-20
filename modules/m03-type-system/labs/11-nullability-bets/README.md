# Game 3b — Nullability bets

**Lecture:** after [3.4 Nullability and lists](../../lectures/3.4-nullability.md) · **Grading:** auto + rubric · **Needs:** nothing · **Kind:** game

Eight Shelfie fields. For each, bet: **nullable** or **non-null**. Put 1 to 3 points on each bet,
**16 points in total**. Then a failure scenario per field settles it: a right bet wins its stake, a
wrong bet loses it. Score 8 or more to pass. The scenarios live in `tests/key.json`: do not open it, run the test.

| # | Field | Context |
|---|---|---|
| 1 | `Book.title` | Same database row as the book's id; required when a book is created |
| 2 | `Book.coverUrl` | Cover images come from a third-party CDN, fetched by URL when the book is shown |
| 3 | `Club.owner` | The user who created the club |
| 4 | `Review.author` | The user who wrote the review |
| 5 | `Book.averageRating` | Mean of a book's review ratings |
| 6 | `User.handle` | Chosen at sign-up, unique, required |
| 7 | `Club.memberCount` | Computed from the club's member list |
| 8 | `Club.currentBook` | The book the club is reading now |

## Do

1. Fill `bets.json`: `{"Book.title": {"bet": "non-null", "stake": 2}, ...}`, all 8, stakes 1 to 3,
   summing to 16.
2. One sentence per bet in `reasons.md`, **before** you run the test.
3. `bin/course test m03/11` settles the bets and prints each scenario.

Changing bets after seeing the scenarios is allowed and pointless: the rubric grades `reasons.md`.

## Group variant

Everyone fills `bets.json` alone, then reveal one field at a time: read the bets aloud, run the
scenario, settle. The highest total wins. A bet anyone disputes after the scenario gets two minutes of
argument; the group may overrule the scenario by majority, but only with a better scenario.

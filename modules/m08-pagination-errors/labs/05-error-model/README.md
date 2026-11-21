# Lab 8.5 — Where does the null go?

**Lecture:** [8.4 The error model](../../lectures/8.4-error-model.md) · **Grading:** both · **Needs:** nothing

Ten requests against one small schema. Three are **request errors**: they never execute. Seven throw
during **execution**, each at a different depth. You make the server throw, predict every response
before you see any of them, and then explain what you got wrong.

## Do

1. In `Depths.java`, add six throws to the wiring (any `RuntimeException`). Only the throwing field
   and the object it throws for are fixed; leave every other fetcher alone:

   | Case | Field that throws | For |
   |---|---|---|
   | E1 | `Query.hello` | always |
   | E2, E6 | `Club.owner` | every club |
   | E3 | `User.name` | user `u3` only |
   | E4 | `Book.title` | book `b2` only |
   | E5 | `Book.rating` | book `b1` only |
   | E7 | `Review.body` | review `r2` only |

   `CASES` holds the ten queries; don't edit them. Read `SDL` closely: the `!`s decide everything.
2. **Before you run anything**, fill in `predictions.json`: for each case, the whole `data` value you
   expect (or `"absent"` when there is no `data` key) and the `path` of each error (`[]` for an error
   without a path). For example, `{"data": {"club": null}, "errorPaths": [["club", "x"]]}`. Commit it,
   or at least copy it somewhere, so you can't fix it up afterwards without knowing you did.
3. Run `bin/course java Depths.java` and compare.

## Done when

`bin/course test m08/05` is green. It checks that each error comes from the right place and that
every case has a prediction. It reports how many predictions match without grading them: wrong
predictions are what `notes.md` is for, and `bin/course grade m08/05` grades the explanations.

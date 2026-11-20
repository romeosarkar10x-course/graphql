# Game — GraphQL golf

**Module 2 game** · **Grading:** auto (strokes = characters) · **Needs:** `bin/course ref up`; a `GITHUB_TOKEN` for the bonus holes

Each hole is a target response. Write the shortest document that returns exactly that `data`. Every
character in the file counts (spaces, newlines and commas included) except one trailing newline.

```bash
bin/course start m02/09      # then fill in holes/01.graphql ... holes/08.graphql
bin/course test m02/09       # checks each hole and counts strokes
```

| Hole | Target `data` (full JSON in `tests/targets/NN.json`) |
|---|---|
| 01 | `{"hello":"Hello from Shelfie!"}` |
| 02 | The names of all clubs, as `{"clubs":[{"name":...}, ...]}` |
| 03 | The titles of every book whose title contains "the" |
| 04 | `{"a":{"name":"The Midnight Readers"},"b":{"name":"Slow Burn Mysteries"}}` |
| 05 | Every club's owner's handle: `{"clubs":[{"owner":{"handle":...}}, ...]}` |
| 06 | Search results for "ur", each with `__typename` and `id` |
| 07 | The names of the authors of book b1's three newest reviews |
| 08 | `{"clubs":[{"n":8},{"n":4},{"n":5}]}` |
| g1 | Bonus, GitHub: `{"repository":{"stargazerCount":...}}` for graphql/graphql-spec |
| g2 | Bonus, GitHub: `{"user":{"name":...,"company":...}}` for torvalds |

Targets live in `modules/m02-query-language/labs/09-golf/tests/targets/`. Reading them is allowed:
it is golf, not a guessing game.

**What you learn:** which characters are syntax and which are noise (commas and most whitespace are
insignificant), that aliases rename, that a union needs a type condition, and that the target depends
on the data: look at what hole 06 actually returns.

**Solo:** get under par on every hole. **Group:** 15 minutes, lowest total strokes wins; ties go to
whoever finished first. Record results in
[games/leaderboards/graphql-golf.md](../../../../games/leaderboards/graphql-golf.md).

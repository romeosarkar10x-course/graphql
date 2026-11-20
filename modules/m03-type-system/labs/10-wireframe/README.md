# Game 3a — Wireframe to schema

**Lecture:** after [3.9](../../lectures/3.9-extensions-coordinates.md) · **Grading:** auto + rubric · **Needs:** nothing · **Kind:** game

Four screens of the Shelfie app. Design the schema that serves each screen in **one request**, and
write that request. Then compare with the model answer (shown by the test after you pass).

## The screens

```
1. CLUB PAGE                                 2. BOOK PAGE
+------------------------------------------+  +------------------------------------------+
| [banner image]                           |  | [cover]  The Left Hand of Darkness       |
| The Midnight Readers        8 members    |  |          Ursula K. Le Guin · 1969         |
| Owner: (o) Kemi Adeyemi                  |  |          ★ 4.2 (5 reviews)                |
| (o)(o)(o)(o)(o) +3            [ Join ]   |  |          My shelf: [ Reading       v ]    |
|                                          |  |------------------------------------------|
| Reading now                              |  | Reviews                                  |
| [cover] The Left Hand of Darkness ★ 4.2  |  | (o) Hugo Silva ★4  [spoiler: mild]       |
|         Ursula K. Le Guin                |  |     "Estraven deserved better."          |
| Next meetup                              |  | (o) Jonas Berg ★3                        |
| Thu 27 Nov, 19:00 · Blossom Book House   |  |     "Beautiful but I wanted more ..."    |
| "Left Hand of Darkness, part 2"          |  |      [ more reviews ]                    |
+------------------------------------------+  | Clubs reading this: Midnight Readers (8) |
                                              +------------------------------------------+
3. PROFILE                                   4. SEARCH  [ le guin          ]
+------------------------------------------+  +------------------------------------------+
| (o) Asha Rao  @asha  · Bengaluru         |  | 3 results                                |
| "Reads sci-fi on the night bus."         |  | BOOK  [c] The Left Hand of Darkness      |
| 41 books read · 5 reviews                |  |           Ursula K. Le Guin              |
| [Reading] [Want to read] [Finished]      |  | BOOK  [c] The Dispossessed               |
| [c][c][c][c]                             |  |           Ursula K. Le Guin              |
| Clubs: The Midnight Readers,             |  | CLUB  Le Guin Appreciation  · 14 members |
|        Booker Shortlist Survivors        |  | USER  (o) Ursula Fan @leguinfan          |
+------------------------------------------+  +------------------------------------------+
```

`(o)` is an avatar, `[c]` a cover, `[ Join ]` depends on whether the signed-in user is a member,
"My shelf" is the signed-in user's status for this book.

## Do

1. `schema.graphqls`: every type, field and argument the four screens need. Use what Module 3
   taught: scalars, enums, an interface or union where it fits, nullability you can defend.
2. `screens/01-club.graphql` to `screens/04-search.graphql`: one named operation per screen, with
   variables, selecting exactly what the screen shows.
3. `DESIGN.md`: the three decisions you are least sure about.

The test checks that the schema builds and each screen's operation validates against it. The
rubric checks the design.

## Group variant

20 minutes, alone, no talking. Then pair up and diff: `bin/course gql schema-lines` both schemas and
`diff` them. For every difference, each side gets 30 seconds to argue. Score a point for each
difference the other person agrees to adopt.

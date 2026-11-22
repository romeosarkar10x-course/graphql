# Game 17.10: Spec scavenger hunt

Lecture: [17.1 Reading the spec](../../lectures/17.1-spec.md). Grading: auto (12 of 15 to pass).

Fifteen questions only the text of the **September 2025 edition** answers (https://spec.graphql.org/September2025/).
Not a library, not a blog post, not memory: the spec. Put each answer in `answers.json`, short, as asked: a section
number as printed (`5.3.2`), a name exactly as the spec writes it, a number, a code point as `U+XXXX`. Case, a
leading `§`, `@` and quotes do not matter. The test says which are not right yet, never what they should be.

| Id | Question |
| --- | --- |
| q01 | Which section defines `CollectFields()`? |
| q02 | Which algorithm does `ExecuteRequest()` call first? |
| q03 | The default `reason` of `@deprecated`? |
| q04 | The largest value an `Int` can hold, as digits? |
| q05 | The built-in directive that makes an input object take exactly one field? |
| q06 | How many hex digits does a fixed-width `\u` escape have? |
| q07 | The code point of the ignored character that may appear before or after any token "as files are often concatenated"? |
| q08 | Which section says two fields with the same response name must have the same field name and arguments? |
| q09 | The two directives a subscription's root selection set may not use (both, any order)? |
| q10 | The response entry "reserved for implementers to extend the protocol"? |
| q11 | The operation type whose root selection set "should be executed serially"? |
| q12 | `BlockStringValue()` joins the lines with which code point? |
| q13 | Which section defines schema coordinates (`Type.field(arg:)`)? |
| q14 | The letter of the appendix that summarises the grammar? |
| q15 | The algorithm 5.8.5 uses to decide whether a variable may be used at a position? |

```bash
bin/course start m17/10
bin/course test m17/10
```

## Solo

Against the clock: start a timer, stop it at 15 of 15. Write down, per question, where you looked first. The
lesson is which questions the table of contents answers and which need the algorithms.

## Group (2 to 6 players)

Everyone starts the same lab. Each player writes `answers/<name>.json` (same shape as `answers.json`) in one
shared copy, in 25 minutes. `bin/course test m17/10` then prints a leaderboard: most right first, ties to the
earlier file. Afterwards, each player explains one question they got and one they missed, with the spec open.

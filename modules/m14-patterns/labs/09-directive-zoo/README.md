# Game 14.9: Directive zoo

Lecture: [14.1 Custom schema directives](../../lectures/14.1-directives.md). Grading: auto + rubric. Needs nothing.

The zoo has three animals with exact rules (their descriptions in `zoo.graphqls`), and room for yours:

- `@pirate`: a word table, capitals kept, " Arr!" at the end.
- `@emojify`: another word table, emoji instead of words.
- `@redactOnFridays(replacement:)`: the field's value is the replacement on Fridays, in the server clock's zone
  (`shelfie.zone`, Europe/London). The clock is a bean the tests pin with `--shelfie.clock=<instant>`.

`Quote.shanty` carries `@emojify @pirate`: order matters.

```bash
bin/course start m14/09
cd workspace/m14-patterns/09-directive-zoo
bin/course shell -c 'mvn -q package && java -jar target/shelfie-zoo.jar --shelfie.clock=2026-11-27T10:00:00Z'
bin/course test m14/09
```

## Solo

Make the three animals pass on a Thursday and on a Friday that is still Thursday in UTC. Then add **your own
exhibit** to `zoo.graphqls`, below the marked line: a `directive @...` with a description precise enough that
someone else could implement it, used on a field (`extend type Quote { ... }`). `ZOO.md` says what it does, how it
stacks with one of the others, and what you learned. Ideas: `@yell(times:)`, `@spoonerise`, `@rot13`,
`@haiku` (refuse lines that are not 5-7-5, at start-up if you dare), `@lateAtNight(after:)`.

## Group variant

Each player brings their exhibit and its description, but not its code. Players swap descriptions and implement
someone else's directive from the description alone in 20 minutes; the author then runs their own five test
queries against it. Points: one per passing query for the implementer; for the author, one point per query that
the description made unambiguous (the group votes on each failure: bad implementation or bad description?). Then
everyone adds every directive to one shared schema and stacks all of them on one field: whose order wins?

## Done when

`bin/course test m14/09` passes (the three animals, your exhibit declared and used) and `ZOO.md` passes the rubric
(`bin/course grade m14/09`).

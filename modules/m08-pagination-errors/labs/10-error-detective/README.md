# Game 8b — Error detective

**Lectures:** [8.4 The error model](../../lectures/8.4-error-model.md), [8.5 Error strategy](../../lectures/8.5-error-strategy.md) · **Grading:** auto · **Needs:** nothing

Six support tickets in `incidents/`. Each has what the user reported, the request, the response, and an
excerpt of the server log. For each one, find **what went wrong** and **the field where it started**.
That field is often not the one the user complained about.

The incidents are staged: `tests/gen/Incidents.java` builds a small Shelfie on graphql-java 26.1, plants
one fault per incident, and runs the request. Every response and log line in `incidents/` is that
program's real output. (Don't read the generator before you play: it is the answer key in code.)

## Causes

| | Cause |
|---|---|
| A | The database is down. |
| B | A record has `null` in a field the schema declares non-null, and the null propagated up. |
| C | A fetcher returns a value of the wrong shape (an id string instead of the object). |
| D | A field was removed or renamed while old clients still request it. |
| E | The client sends a variable of the wrong type. |
| F | Stored data the code cannot parse makes one fetcher throw. |
| G | A slow fetcher exceeded the request's time budget. |
| H | The viewer is not allowed to see the object. |
| I | The client's query has a syntax error. |
| J | The object store (S3) is unreachable. |

## Play (solo)

Write `answers.md`, one line per incident: number, cause letter, and the **schema coordinate** where it
started: `Type.field`, or `Type.field(argument:)` for an argument (Module 2). For example, `7: A Query.club`.
Score: 1 point per right cause and 1 per right origin. 10 of 12 passes. The test tells you which
answers are wrong, not what is right. Aim to finish in 20 minutes.

## Play (group, teams of 2)

1. 15 minutes, one `answers.md` per team, laptops closed to other teams.
2. Score with `bin/course test m08/10`. Ties: the team that finished first.
3. Each team then explains one incident to the room, in the order of the scoreboard (winners pick
   first). The explanation must name the log line or response field that gave it away, and say
   what the server should have done differently: a schema fix, a code fix, or an error-contract fix.

## Done when

`bin/course test m08/10` shows at least 10 of 12 points.

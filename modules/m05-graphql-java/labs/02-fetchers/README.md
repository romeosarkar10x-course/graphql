# Lab 5.2 — Every fetcher, and what it is handed

**Lecture:** [5.2 Data fetchers in depth](../../lectures/5.2-data-fetchers.md) · **Grading:** auto + rubric · **Needs:** nothing

`CallTree.java` wires `shelfie.graphqls` plus `lab.graphqls` to `Data.java` by hand. A provided
`Tracing` instrumentation calls `record(env)` before **every** fetcher, including the default ones.

## Part 1: the call tree

`bin/course java CallTree.java tree ../tests/queries/three-levels.graphql` must print exactly
(first lines shown; the test compares all of them):

```
club(id: "c1")  <- root  selects [name, owner, members]
  name  <- Club
  owner  <- Club
    handle  <- User
  members(first: 2)  <- Club
    [0] handle  <- User
    [0] reviews  <- User
    ...
```

One line per call: two spaces per level, `[i] ` when the parent is a list item, the response key, the
arguments sorted by name (defaults included, strings quoted), `  <- ` and the source's simple class
name (`root` at the top), and for root fields `  selects [...]`, the immediate sub-field names.

## Part 2: three fetchers that need more than the source

| Field | Must read | Behaviour |
|---|---|---|
| `Query.viewer` | `GraphQLContext` key `viewer` (`run <file> u5` puts it there) | the user, or null |
| `User.roleInClub` | the club this user was reached through | `OWNER` / `MEMBER` under `Club.owner` and `Club.members`, null elsewhere |
| `Query.booksById` | — | one entry per id, null for unknown ids, plus one error per gap with `path` `["booksById", i]` and `extensions.code` `NOT_FOUND` |

## Part 3: notes.md

Four questions about what the tree shows.

## Done when

`bin/course test m05/02` is green and `bin/course grade m05/02` gives 7/10 or more.

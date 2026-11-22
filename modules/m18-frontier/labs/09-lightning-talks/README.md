# Game 18.9: Lightning talks

Lecture: [18.5 Where the spec is heading](../../lectures/18.5-spec-frontier.md). Grading: rubric.

Five minutes on one RFC or one ecosystem tool, for people who know GraphQL but not your topic. Pick from:

- an open RFC: incremental delivery (#1110), error behaviors (#1163), fragment arguments (#1081), service
  capabilities (#1208), Full Schemas, Composite Schemas, persisted documents over HTTP (graphql-over-http #264);
- a tool or platform from this module or earlier: MCP and Apollo MCP Server, Spring AI's MCP server, PostGraphile,
  Hasura, pg_graphql, the Neo4j GraphQL Library, Dgraph, Viaduct, graphql-kotlin, Pothos, gqlgen, Hive, a router.

Rules: five minutes hard stop, at most five slides (or none), one live demo or one real request and response,
every claim with a source you read yourself, and the date you read it (things move: say what stage or version).

Write it in `TALK.md`: the script (about 600 to 750 words, which is five minutes spoken), the slide outline,
the demo, and the sources.

```bash
bin/course start m18/09
bin/course grade m18/09     # prints the rubric prompt for an AI grader, with your TALK.md
```

## Solo

Record yourself (a phone is fine) and watch it once at 1x. Then grade `TALK.md` with `bin/course grade m18/09`
and note in `TALK.md` where the recording ran over or lost the thread.

## Group

One session, one talk each, a timer everyone can see. After each talk, every listener scores it on the
rubric's four rows (0 to 2 each) on a card or in a shared sheet, plus one question they still have. The speaker
answers the questions in writing afterwards, in `TALK.md`. Highest mean score picks next session's topic.

## Done when

`TALK.md` passes the rubric (`bin/course grade m18/09`).

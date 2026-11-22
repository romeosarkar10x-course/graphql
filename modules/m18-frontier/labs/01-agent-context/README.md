# Lab 18.1: Ten operations an agent can read

Lecture: [18.1 GraphQL for AI agents](../../lectures/18.1-agents.md). Grading: auto + rubric. Needs the reference
server.

An agent that calls Shelfie reads two things: the schema, and the operations you let it run. Since the September
2025 edition an operation can carry a description (RFC #1170), and that description is the tool's documentation:
the agent sees nothing else when it decides which tool to call and what to pass.

`operations/` holds ten Shelfie operations, eight reads and two writes, with no descriptions. Labs 18.2 and 18.3
turn them into MCP tools.

1. **Describe all ten.** A `"""block string"""` before `query` or `mutation`, 12 to 120 words, that says what the
   operation returns, names every variable (`$id`, with an example value and where to get it), and says what the
   agent needs to know to use it right: defaults that hide data, limits, what `null` means, and for the two
   mutations what changes and how problems come back. Do not change the operations themselves.
2. **Let an LLM answer ten questions.** Give any model you have Shelfie's schema
   (`curl localhost:8080/graphql/schema`) and, in a second run, your ten described operations too; ask it for
   one GraphQL query per question in [questions.json](../../contract/agent/questions.json). Save the queries of the
   run you want scored as `llm/q01.graphql` to `llm/q10.graphql` (one operation each, values written in, no
   variables) and score them:

   ```bash
   bin/course shell -c 'source /course/tools/testlib.sh; source /course/modules/m18-frontier/contract/agent/agent.sh;
     cd /course/workspace/m18-frontier/01-agent-context && agent score /course/modules/m18-frontier/contract/agent/questions.json llm'
   ```

   The scorer checks each query against the schema, prices it with Module 10's cost function (a field costs 1
   plus its children, times `first` or `last`), runs it, and says whether the response holds the answer's values
   (checked by hash; it never prints them). Whether the model then reads the answer right is yours to judge.
3. **Write `REPORT.md`:** the scores of both runs, the queries that failed and why, what the descriptions changed,
   and the riskiest query the model wrote.

Graphql-java 26.1, which the reference server runs, rejects descriptions on operations (Module 17's finding 6).
The test parses your files with Module 17's engine, which follows the edition, then strips the descriptions and
validates the rest with graphql-java against Shelfie. Lab 18.2's server has to strip them the same way before it
sends an operation.

```bash
bin/course start m18/01
bin/course test m18/01
```

## Done when

`bin/course test m18/01` passes (ten described, valid operations; ten scored queries; `REPORT.md` filled in) and
`REPORT.md` passes the rubric (`bin/course grade m18/01`).

No LLM at hand? Write the ten queries yourself, from the schema alone and without running them, and score
those: you are the model. Say so in the report.

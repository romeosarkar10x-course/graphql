# Rubric, lab 1.4 tradeoff-matrix.md (15 points, 3 per scenario)

Per scenario:

| Points | Full marks |
|---|---|
| 1 | A pick, with two reasons drawn from real criteria (caching, tooling, typing, streaming, browser reach, team topology, public vs private) rather than slogans |
| 1 | The reasons fit the scenario's clients (who calls it, how many, who controls them) |
| 1 | A sensible runner-up with a concrete condition that would flip the decision |

The reference picks (REST or GraphQL, gRPC, GraphQL, tRPC, OData) are not the only acceptable ones:
grade the argument, not the label. "GraphQL for everything" with no trade-offs named scores at most 1 per scenario.

Pass: 11/15.

# Rubric, lab 1.4 change-impact.md (12 points)

| Criterion | Points | Full marks |
|---|---|---|
| REST | 3 | Names the files that change; weighs new endpoint (extra request) against growing an existing resource (over-fetching for everyone); says nothing breaks |
| gRPC | 3 | New field/message in `.proto` plus implementation; old clients skip unknown fields; notes the screen RPC must be extended by the backend team |
| GraphQL | 3 | New type/field in SDL plus a resolver; existing queries get identical responses; still one request |
| Verdict | 3 | Says who saves cost (client teams) and where it moves (server: efficient resolving, authorization) |

Pass: 9/12.

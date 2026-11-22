# Module 18 — The frontier: AI agents, open RFCs and other languages

About 7 hours, 6 lectures. Shelfie meets AI agents: operations described for them (September 2025's executable
descriptions), an MCP server in Java whose tools are those operations, and a safety policy in front of it. Then
an API generated from the database instead of written, a map of where the spec is going, and a Shelfie
subgraph ported to TypeScript that passes the same Hurl suite and composes into the same supergraph.

## Before you start

Labs 18.1 to 18.3 and 18.7 need the reference server; 18.4 needs PostgreSQL and Node; 18.6 and 18.8 need Node
and the federation tools (`bin/course fed`). npm installs need internet once. No LLM account is required: lab
18.1 accepts queries you write yourself, and the MCP labs are tested with the course's own MCP client.

Shared material in [contract/](contract/):

- `agent/`: the ten operations without descriptions (labs 18.1 to 18.3), `questions.json` (ten questions, answers
  hashed), and `Agent.java` (`agent.sh`): `agent ops` (checks described operations with Module 17's
  September 2025 parser and graphql-java), `agent strip`, `agent cost` (Module 10's function), `agent score`.
- `mcp.sh`: a small MCP client in bash (curl and jq; Streamable HTTP, protocol 2025-11-25): `mcp_init`,
  `mcp_tools`, `mcp_call`, `mcp_text`. Every MCP test uses it, so a server in any language is graded the same way.
- `redteam/attacks.sh`: mini-project 18.7's twelve attacks, and the format for your own.
- `port/`: the reviews subgraph's contract for lab 18.6 and game 18.8: `signatures.txt` (15 field signatures,
  from Java), `seed.json`, `port.hurl` (19 requests, which Java's subgraph passes too), `port.sh`, `router.yaml`.

## Lectures and labs

| # | Lecture | Lab | Grading |
|---|---|---|---|
| 18.1 | [GraphQL for AI agents](lectures/18.1-agents.md) | [m18/01 ten described operations; an LLM scored on ten questions](labs/01-agent-context/README.md) | auto + rubric |
| 18.2 | [An MCP server in Java](lectures/18.2-mcp-server.md) | [m18/02 Spring AI tools that run persisted operations](labs/02-mcp-server/README.md) | auto + rubric |
| 18.3 | [Agent safety](lectures/18.3-agent-safety.md) | [m18/03 read-only default, guarded queries, cost cap, human approval, the caller passed through](labs/03-agent-safety/README.md) | auto + rubric |
| 18.4 | [Platforms beyond hand-built servers](lectures/18.4-platforms.md) | [m18/04 PostGraphile on the club database, and what it lacks](labs/04-platforms/README.md) | auto + rubric |
| 18.5 | [Where the spec is heading](lectures/18.5-spec-frontier.md) | [m18/05 seven stages, two RFCs read, a wiki explainer](labs/05-spec-frontier/README.md) | auto + rubric |
| 18.6 | [Porting to another language](lectures/18.6-porting.md) | [m18/06 the reviews subgraph in TypeScript](labs/06-port/README.md) | auto + rubric |

Labs chain: 18.1 → 18.2 → 18.3 → mini-project 18.7; 18.6 → game 18.8.

## Mini-projects and games

| | Solo | Group |
|---|---|---|
| [m18/07 Agent safety lab](labs/07-project-red-team/README.md) (mini-project) | find the seven seeded weaknesses blind, then the course's 12 attacks; points per bypass found and fixed | teams attack each other's published servers with their own attacks |
| [m18/08 Polyglot day](labs/08-polyglot-day/README.md) (game) | Java, your Kotlin (15.10) and your TypeScript (18.6) in one supergraph | five languages or more, one team each, the referee's `team.json` |
| [m18/09 Lightning talks](labs/09-lightning-talks/README.md) (game) | a five-minute talk, recorded and self-graded | one session, peer scores on the rubric |

## Changes from the plan

- **18.1's experiment accepts any model, or none.** The course has no LLM account; `agent score` grades any ten
  queries. The solution's sample run was written by Claude Opus 5.5 while it built the lab, after it had seen the
  data: it shows the shape of an answer, not a measurement.
- **18.2's question changed.** The plan asks "which club read the most books last month?"; the reference
  server's seed has no reading history (and the seed is load-bearing for earlier labs), so the lab asks "Which
  public club is reading the highest-rated book right now?", which needs the same reasoning over tools.
- **Connecting a real assistant is the learner's step.** No assistant was connected while the module was built;
  the MCP labs are tested with `contract/mcp.sh`. The Claude Code command in lab 18.2 is from its documentation.
- **Viaduct was not run.** Its starter is Gradle-based upstream and the authoring session was not allowed to run a
  Gradle wrapper. Lab 18.4 gives the README's command and asks you to run it; the lecture describes Viaduct from
  its README and starter sources.
- **The port is TypeScript.** Go (gqlgen) is allowed by the plan but has no reference port and no Go toolchain
  in the course image, so the test path for Go is unverified.
- **18.4's other platforms** (Hasura, pg_graphql, Neo4j, Dgraph) are lecture-only; licences and versions were
  checked on GitHub, behaviour is from their documentation.

## Spec references

- [GraphQL, September 2025 edition](https://spec.graphql.org/September2025/): executable descriptions (18.1),
  5.2.1.1 Operation Type Existence (18.4).
- [RFC tracker](https://rfcs.graphql.org/rfcs/), read 2026-11-23: #1110, #1163, #1081, #1208, Full Schemas,
  Composite Schemas (18.5); [graphql-over-http #264](https://github.com/graphql/graphql-over-http/pull/264)
  (persisted documents).
- [Model Context Protocol, 2025-06-18](https://modelcontextprotocol.io/specification/2025-06-18) (transports,
  elicitation, authorization); the current version is 2026-07-28 (18.2, 18.3).

## Checkpoint

`bin/course checkpoint 18` (see [checkpoint.md](checkpoint.md)): the MCP server with its safety policy (lab 18.3)
and the ported subgraph (lab 18.6), plus `REDTEAM.md` and `PORT.md`. Tag: `checkpoint-18`.

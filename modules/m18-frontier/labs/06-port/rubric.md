# Rubric: 18.6 Porting a subgraph to another language (PORT.md)

Pass mark: 6 of 10.

| Points | Criterion |
| --- | --- |
| 3 | What did not carry over unchanged, each with the fix: defaults (nullability, argument order), federation directives (`@extends`, the link version), error shapes, dependency versions |
| 2 | Idioms: how the code-first schema, the context (the reader) and the entity resolvers map to Spring for GraphQL's annotations |
| 3 | Numbers beside Java's reviews subgraph: startup time, resident memory, and p50/p95 for one query over 1,000 requests, with the command used |
| 2 | Would you keep this subgraph in TypeScript? A team-level argument, not a benchmark one |

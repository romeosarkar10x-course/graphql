# Lab 18.5: Where the spec is heading

Lecture: [18.5 Where the spec is heading](../../lectures/18.5-spec-frontier.md). Grading: auto + rubric. Needs
internet (the RFC tracker and GitHub).

The September 2025 edition is where Modules 1 to 17 stand. The next edition is being written in the open on the
[RFC tracker](https://rfcs.graphql.org/rfcs/): each proposal has a stage (RFC 0 Strawman, 1 Proposal, 2 Draft,
3 Accepted) and a champion, and moves when the working group agrees.

1. **`stages.json`:** look up the stage today of the seven proposals the lecture covers: incremental delivery
   (`@defer`/`@stream`), error behaviors (`onError`), fragment arguments, service capabilities, Full Schemas,
   Composite Schemas, and persisted documents in GraphQL over HTTP (that one lives in the
   [graphql-over-http](https://github.com/graphql/graphql-over-http) repository: is it an open pull request,
   merged, or in the September 2026 edition?).
2. **Read two open RFCs end to end** (`READING.md`): not the summary, the pull request's spec diff and its
   discussion. Pick two that relate (the lecture suggests pairs).
3. **Write a 250 to 350 word explainer for the class wiki** (`EXPLAINER.md`), linking both RFCs: the problem,
   what changes for clients and servers, where it is in the process, and what it would mean for Shelfie.

```bash
bin/course start m18/05
bin/course test m18/05
```

The test's stage answers are hashed and dated: they were right on 2026-11-23. Stages move; if a check fails and
you are sure the tracker says otherwise, say so in `READING.md` and tell the course maintainer.

## Done when

`bin/course test m18/05` passes and `READING.md` and `EXPLAINER.md` pass the rubric (`bin/course grade m18/05`).

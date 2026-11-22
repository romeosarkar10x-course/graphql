# Lab 17.8: GraphQL over HTTP in depth

Lecture: [17.8 GraphQL over HTTP](../../lectures/17.8-http.md). Grading: auto + rubric. Needs Node (the course's,
`"needs": ["node"]`) and internet once (npm installs the audit).

Module 5 built a server from the September 2026 HTTP edition and checked it with 25 Hurl checks. Now audit it with
[graphql-http](https://github.com/graphql/graphql-http) 1.23.1 (MIT), the GraphQL Foundation's audit suite: 61
checks, each a MUST, SHOULD or MAY with an id. The starter is Module 5's mini-project server, unchanged.

- Fix every MUST (`error`) and every SHOULD (`warn`) the edition agrees with.
- Four warnings and the 18 MAY notices ask for status codes the September 2026 edition contradicts
  (graphql-http 1.23.1 was released before the edition). For those, follow the edition and say why in `AUDIT.md`,
  quoting it. Deciding which those are is part of the lab: do not take graphql-http's word, or the course's.
- Keep Module 5's 25 checks green.
- Refuse a batch (a JSON array of requests) with a 4xx, without running it.

"100%" here means: no MUST broken, and every remaining warning or notice explained by the edition's text.

```bash
bin/course start m17/08
bin/course test m17/08
# by hand, inside the image:
cd workspace/m17-under-the-hood/08-http && mvn -q package && java -jar target/shelfie-server.jar 4000 &
node /course/.cache/m17-http-audit/audit.mjs http://localhost:4000/graphql   # after one bin/course test
```

The edition is in `_workbench/2026-11-21-m05-probe/GraphQLOverHTTP-September2026.md` and at
https://http-spec.graphql.org/September2026/.

## Done when

`bin/course test m17/08` passes and `AUDIT.md` passes the rubric (`bin/course grade m17/08`).

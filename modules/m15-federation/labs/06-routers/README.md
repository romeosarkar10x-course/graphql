# Lab 15.6: Routers and gateways

Lecture: [15.6 Routers and gateways](../../lectures/15.6-routers.md). Grading: auto + rubric. Needs the federation
tools (`bin/course fed`: Apollo Router 2.19.0 and Hive Router 0.3.9, fetched and checksummed into `.cache/fed`).

Two routers, one supergraph, one set of production requirements. Write `apollo.yaml` (Apollo Router, Elastic
License 2.0, free to run unlicensed for the features used here) and `hive.yaml` (Hive Router, MIT) so that each:

1. **forwards `Authorization` and `X-Shelfie-User`** to every subgraph, and nothing else (a client's `Cookie`
   must not reach them);
2. **refuses introspection**;
3. **keeps query plans private** (the course's contract configs expose them; production ones must not);
4. **times out a subgraph after 2 seconds**: when reviews hangs, a book page answers in under 2.5 s with its title
   and `averageRating: null` plus an error, instead of hanging.

The test runs the contract's four subgraphs, starts each router with your file, checks the four rules through it,
then the experiment: 2000 requests, 16 at a time, through each router
([contract/Load.java](../../contract/Load.java)), and prints latency percentiles and the router's memory. Write
the table into `notes.md` with what you make of it.

```bash
bin/course start m15/06
cd workspace/m15-federation/06-routers
bin/course shell -c 'ROUTER_CONFIG=$PWD/apollo.yaml bash /course/modules/m15-federation/contract/stack.sh apollo'
bin/course test m15/06
```

The two config dialects differ in almost every key: the routers' own documentation is in
`_workbench/2026-11-22-m15-probe/docs/` for the versions pinned here (Hive's full JSON Schema included).

## Done when

`bin/course test m15/06` passes and `notes.md` passes the rubric (`bin/course grade m15/06`).

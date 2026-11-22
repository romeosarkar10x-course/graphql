# Mini-project 13.10: A schema-driven mock server

Lecture: [13.4 Mocking](../../lectures/13.4-mocking.md). Grading: auto + rubric (DESIGN.md). Needs nothing.

Turn lab 13.4's `MockWiring` into a tool anyone can run against any schema: one standalone jar.

```bash
java -jar target/mock-server.jar <schema.graphqls> [--port 4000] [--seed 13] [--mocks <dir>]
```

- **Any SDL.** The grader runs your jar on four schemas, two of which you have never seen
  (`tests/schemas/social.graphqls`: recursion, nested and nullable lists, mutations; `forge.graphqls`: GitHub-style
  connections, custom scalars, interfaces that implement interfaces, unions), and checks every response with
  [MockCheck](../../contract/mock/MockCheck.java) `--realistic`, as in lab 13.4.
- **Deterministic by seed**, different between seeds; introspection works; POST only (GET is 405); a body that is
  not JSON is a GraphQL error, not a crash.
- **Standalone:** graphql-java and Jackson inside the jar (`maven-shade-plugin`), nothing next to it.
- **MockSpec**, the [GraphQL Response Mocking RFC](https://rfcs.graphql.org/rfcs/MockSpec/) (RFC 0, strawman):
  with `--mocks <dir>`, an operation that carries `@mock` or `@mock(name: "x")` is answered from
  `<dir>/<OperationName>.json`: the `__default__` entry or the named one, without `__description__`. An unknown
  name is an error listing the names there are; a missing file is generated from your fake data and written as
  `{"__default__": ...}`; a mock that no longer fits the operation is refused (the RFC's check: run the operation
  with no resolvers and the mock's data as the root value). Add `directive @mock(name: String) on QUERY |
  MUTATION | SUBSCRIPTION` to schemas that lack it. The RFC is written for clients; reading it in a server lets
  any client use the same files. [tests/mockspec.hurl](tests/mockspec.hurl) has the 12 checks.

```bash
bin/course start m13/10
cd workspace/m13-testing/10-project-mock-server
bin/course shell -c 'mvn -q package && java -jar target/mock-server.jar /course/modules/m13-testing/contract/shelfie.graphqls'
bin/course test m13/10
```

`DESIGN.md`: how values are made, MockSpec in a server, and what a type-only mock cannot do.

## Group variant: schema swap

Each player writes one SDL file and three queries meant to break other mock servers (deep recursion, a union of
interfaces, a list of lists of a custom scalar, `first: 0`, an enum with one value, field names that fool
heuristics). Put everyone's files in `schemas/<name>.graphqls` and `queries/<name>/` and run each jar with
MockCheck:

```bash
bin/course shell -c 'java -jar target/mock-server.jar schemas/alex.graphqls --port 4100 & sleep 3
  java -cp "$(source /course/tools/testlib.sh; lib_cp)" /course/modules/m13-testing/contract/mock/MockCheck.java \
    http://localhost:4100/graphql schemas/alex.graphqls queries/alex --realistic'
```

One point per clean query on someone else's schema, one point to the author per other server their schema breaks
(a schema that is not valid SDL scores nothing). Fix and rerun once; highest total wins.

## Done when

`bin/course test m13/10` passes and `DESIGN.md` passes the rubric (`bin/course grade m13/10`).

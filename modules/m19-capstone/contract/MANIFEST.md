# capstone.json

The course cannot know your product, so your repository tells the checker where everything is and how to run
it. `capstone.json` sits at the root of your capstone repository (`workspace/m19-capstone/capstone/`). The
starter has a template with every key; the reference product's manifest is a complete example.

Paths are relative to the repository. Every command runs from the repository root inside the course image
(`bin/course shell`), so `/course/...` tools, PostgreSQL at `postgres:5432` and the routers from
`bin/course fed` are available.

## Keys by stage

| Key | Stage | What |
| --- | --- | --- |
| `product` | 1 | Your product's name |
| `docs.pitch`, `docs.design`, `docs.adrs`, `docs.review` | 1 | The pitch, the design doc, the ADR directory, the stage 1 review |
| `subgraphs[]` | 1 | `{name, schema, url}`: each subgraph's contract file and, from stage 2, the URL it serves GraphQL on |
| `operations` | 1 | The directory of client operations (`.graphql`; fragments may be in their own files) |
| `hurl.file`, `hurl.variablesFile` | 1 | The conformance suite and an optional `--variables-file` |
| `build` | 2 | One command that builds everything (`mvn -q -DskipTests package`) |
| `run[]` | 2 | `{name, cmd, ready}`: each process, started in order; `ready` is a URL that answers 200 when it is up |
| `api` | 2 | The URL clients use: the router from stage 4, your one service before |
| `send` | 2 | `query` (the document text) or `documentId` (a persisted id, stage 3 on) |
| `trusted.manifest` | 3 | The persisted documents file: `{"<id>": "<document>"}` or Apollo's `{"operations": [{id, name, body}]}` |
| `tokens` | 2 | Role name to `bin/course token` arguments, e.g. `"member": ["ada", "--aud", "my-api"]` |
| `client.cmd`, `client.env` | 3 | The Java client: a command that prints something and exits 0; env values may be `{{token:<role>}}` |
| `ui` | 3 | The UI's main file |
| `docs.attacks`, `docs.load` | 3 | Your self-run attack report and your load test |
| `metrics[]` | 4 | Prometheus endpoints of your services (`http://localhost:4001/actuator/prometheus`) |
| `dashboard` | 4 | A Grafana dashboard JSON (data source uid `prometheus`) |
| `docs.runbook`, `ci` | 4 | The runbook, the CI workflow file |
| `docs.demo` | 5 | The demo script |
| `probes` | 2-4 | Requests the checker sends to see behaviour: below |

## The stack the checker starts

It runs `build`, then each `run` entry in its own process group, waiting up to 120 s for each `ready` URL, and
stops them all at the end. It does not reset data between stages but does start your processes fresh, so make
your services load their seed at start, or give every write in a probe a `{{run}}` value (replaced by a fresh
string per checker run) so a second run does not collide with the first.

Avoid ports 3000 (Grafana), 3200, 4317, 4318 (Tempo) and 9090 (Prometheus): stage 4 runs them. Send traces
over OTLP/HTTP to `http://localhost:4318/v1/traces` and propagate W3C `traceparent`.

## Variables the Hurl suite gets

`api`, `token_<role>` for every role in `tokens`, `token_expired` (the first role's token, expired) and `run`
(fresh per checker run), plus your `hurl.variablesFile`.

## Probes

Each probe names an operation from `operations/` by its name, `variables`, and `as` (a role from `tokens`;
leave it out for an anonymous request). Paths (`connection`, `count.path`) are jq paths into `data`.

| Probe | Stage | Passes when |
| --- | --- | --- |
| `pagination {operation, connection, pageSize, firstVar, afterVar}` | 2 | Page 1 has `pageSize` edges, `hasNextPage` and an `endCursor`; page 2 (after it) is not empty and shares no `node.id` with page 1 |
| `typedError {operation, variables, as, expect}` | 2 | No `errors`, and some object in `data` has `__typename` equal to `expect` |
| `idempotent {operation, variables, as, count: {operation, variables, as, path}}` | 2 | Two identical requests get identical `data`, and `count` (a number, or an array's length) goes up by exactly one |
| `auth [{name, operation, variables, as, token, expect}]` | 3 | At least 4 cases, at least one `allowed` (HTTP 200, no errors) and two `denied` (HTTP 401/403, or errors); `token` adds a `bin/course token` flag such as `--expired` or `--forged` |
| `expensive {operation, variables, as}` | 3 | Refused: errors and no data |
| `n1 {operation, as, small, large, counters: [{url, path}]}` | 3 | The sum of your statement counters (jq `path` into each URL's JSON) grows by at most 2 more for `large` than for `small` |
| `realtime {kind, operation, variables, as, trigger, url}` | 3 | `subscription`: an event arrives within 8 s of the `trigger` operation, over SSE (`Accept: text/event-stream`, at `url` or `api`), or over graphql-transport-ws when `url` is `ws://...` (the document's text is sent, no token). `defer`: the response has incremental parts |
| `crossSubgraph {operation, variables, as}` | 4 | Sent with a fresh `traceparent`, Tempo holds that trace with spans from at least two services within 30 s |

The checker also sends, by itself: `query CapcheckProbe { __typename }` as raw text (stage 3, must be refused),
and `{ _service { sdl } }` to every subgraph URL (stages 2 and 4: the served schema must compose to exactly
the contract's).

# Mini-project 10: Cost meter

Lecture: [10.6 Public API security](../../lectures/10.6-public-api-security.md). Grading: auto. Needs nothing.

Lab 10.6's API reports every request's cost in `extensions`. Build the operator's view: a proxy that
sits in front of the API, reads each response's `extensions.cost`, and shows each client's spend per
minute on a live dashboard.

`CostMeter.java` (single file, `bin/course java`): listens on a port, forwards `POST /graphql` to the API
unchanged (keeping `Content-Type`, `Authorization`, `X-Api-Key`), and serves:

- `GET /meter`: `{"clients": [{"key": "pk_reader_1", "minutes": [{"minute": "2026-11-21T16:40", "cost": 89, "requests": 5, "throttled": 0}]}]}`
- `GET /`: an HTML page that reads `/meter` every couple of seconds and draws it.

A served request spends its `requestedQueryCost`; a `THROTTLED` one spends nothing and counts in `throttled`.

```bash
bin/course start m10/11
bin/course shell     # then, with lab 10.6's API on :4000:
  java -cp "$(source /course/tools/testlib.sh; lib_cp)" CostMeter.java 4001 http://localhost:4000/graphql
bin/course test m10/11
```

## Done when

`bin/course test m10/11` passes. It starts lab 10.6's reference API, sends known traffic through your
meter from three keys (one of them throttled once), and checks the per-client totals, the minute buckets,
that responses pass through unchanged, and that `/` is the dashboard.

## Group variant

One player runs the API and the meter on a projector; everyone else gets a key and a goal ("fetch every
club's reviews", "post 20 reviews") and writes a client in 10 minutes. The meter shows who backs off and
who burns their budget. Then swap: the operator lowers one key's restore rate without warning, and the
clients must notice from `extensions` alone.

# @defer: notes (reference)

## Time to first byte

`contract/club-page.graphql` for c1, one container, 2026-11-21 (`_workbench/2026-11-21-m11-probe/lab06-defer.txt`):

| request | first byte | whole response |
|---|---|---|
| `Accept: multipart/mixed`, with `@defer` | 0.068 s | 1.58 s |
| `Accept: application/json`, with `@defer` (served without deferring) | 1.51 s | 1.51 s |
| `Accept: multipart/mixed`, no `@defer` | 1.51 s | 1.51 s |

The page's header, members and reviews could be painted 1.4 s earlier; the recommendations panel shows
a placeholder until the second part arrives. The total did not get shorter, and the request costs the
server the same work.

## The payloads graphql-java 26.1 produced

```
---
Content-Type: application/json; charset=utf-8

{"data":{"club":{"name":"Night Owls", ... ,"reviews":[...]}},"hasNext":true}
---
Content-Type: application/json; charset=utf-8

{"hasNext":false,"incremental":[{"path":["club"],"label":"recommendations","data":{"recommendations":[...]}}]}
-----
```

This is the earlier draft's shape: each incremental entry carries its own `path` and `label`, and there is
no `pending` or `completed`. RFC 1110 (RFC 2, the current draft) requires the initial payload to list
`pending: [{"id": "0", "path": ["club"], "label": "recommendations"}]`, and later payloads refer to that
`id` in `incremental: [{"id": "0", "data": {...}}]` and close it with `completed: [{"id": "0"}]`. A client
that speaks only RFC 2 cannot read graphql-java 26.1's stream, and the server cannot convert it without
knowing the pending fragments up front, which graphql-java does not report.

## What Spring for GraphQL did, and the fix

With the experimental flag on, Spring's HTTP handler returned `{"data": {...}, "hasNext": true}` and
dropped the deferred part: a JSON client lost the recommendations without an error. `Accept:
multipart/mixed` alone got 415. The fix turns the flag on only when the client names `multipart/mixed`
(`*/*` is compatible with it but means "one JSON body, please"), and a route ordered before Boot's
streams the parts. With the flag off, graphql-java runs `@defer` fragments inline, so every other
client gets the whole result.

## When @defer is worth it

Defer what is slow and below the fold, where the rest of the screen is useful without it: the
recommendations panel, "readers also liked", review statistics. Do not defer what the first paint needs
or what is fast (each part costs framing and a re-render), and remember `@stream` is not available in
graphql-java 26.1 at all.

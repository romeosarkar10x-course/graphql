# Audit notes (reference solution)

## Before and after

- Starter (Module 5's mini-project server): `{"total":61,"ok":31,"warn":9,"notice":18,"error":3}`.
- After: `{"total":61,"ok":39,"warn":4,"notice":18,"error":0}`, and Module 5's 25 Hurl checks still pass.

## What we changed

- **0220, 0221, 0222** (MUST allow null `variables`, `operationName`, `extensions` with `application/json`) and
  **94B0-94B2** (the same, SHOULD, with `application/graphql-response+json`): a JSON `null` parameter is now treated
  as absent. The edition defines the parameters as optional, and null is how JSON says "none".
- **47DE** (`Accept: */*` should get `application/json`) and **80D8** (no Accept should get `application/json`):
  `*/*` and a missing Accept now get `application/json`. The edition requires the client to send Accept ("The
  client MUST include the media type `application/graphql-response+json` in the `Accept` header") and says nothing
  about requests without one, so the choice is the server's; `*/*` accepts every media type, and
  `application/json` is the one a legacy client can read.
- A batch, a JSON array of requests, is refused with 422 and a message naming batching.

## What we left

- **572B, FDE2, 7B9B**: graphql-http wants 200 for parse, validation and variable-coercion failures when the client
  accepts only `application/json`. The September 2026 edition says a legacy client's request "SHOULD be performed
  following the requirements of this specification as if the request had specified
  `Accept: application/graphql-response+json`, except any response that produces a 2xx status code should replace
  the Content-Type header". So those are 400 and 422 for every client.
- **86EE**: graphql-http wants 400 for a variable-coercion failure; the edition: "If CoerceVariableValues() raises a
  GraphQL request error, the server SHOULD NOT execute the request and SHOULD return a status code of 422".
- **18 notices** (MAY use 400 for parameters of the wrong type): such a request is not well-formed ("An HTTP
  request that encodes parameters of the same names but of the wrong type, or that omits required parameters, is
  not a well-formed _GraphQL-over-HTTP request_"), and "If the request is not a well-formed _GraphQL-over-HTTP
  request_, status code `422` SHOULD be used". graphql-http 1.23.1 was released on
  2026-09-28, before the edition (2026-11-12).

## Batching and persisted documents

Batching is not in the edition: one HTTP request carries one GraphQL request. A batch hides one slow operation behind
another's response and defeats per-request limits. A client should send requests in parallel over HTTP/2 instead.
Persisted documents (not in the edition) send a document id instead of `query`; the
server looks the id up in a store it trusts, and can then refuse anything else. Here it would mean a map from id to
document loaded at start-up, a `documentId` parameter accepted in place of `query` for GET and POST, and a 4xx for an
unknown id.

# Authentication: notes (reference)

## The experiment: one query, nine tokens

Measured on 2026-11-21 against this solution (`_workbench/2026-11-21-m10-probe/auth-probe.txt`), query
`{ viewer { id name roles } }`.

| Token | HTTP status | What the server said (WWW-Authenticate or the GraphQL error) |
|---|---|---|
| none | 200 | `{"data":{"viewer":null}}`: anonymous is allowed, it just has no viewer |
| Keycloak, max | 200 | `viewer` is r3 Max with roles MEMBER, MODERATOR |
| minted, ada | 200 | `viewer` is r1 Ada: the app cannot tell a minted token from Keycloak's, because they share the key |
| expired | 401 | `Bearer error="invalid_token", error_description="... Jwt expired at 2026-11-21T14:58:10Z"` |
| forged (wrong key) | 401 | `... Signed JWT rejected: Invalid signature` |
| alg none | 401 | `Unsupported algorithm of none` |
| wrong audience | 401 | `... The aud claim is not valid` |
| wrong issuer | 401 | `... The iss claim is not valid` |
| `Bearer abc.def.ghi` | 401 | `... Malformed token` |
| `Basic ...` | 200 | viewer null: Spring's bearer filter ignores other schemes, so the request is anonymous |

Every refusal came with an empty body and a `WWW-Authenticate` header, before GraphQL parsed the query.
Spring Security 7.2 also adds `resource_metadata="http://localhost:4000/.well-known/oauth-protected-resource"`
(RFC 9728), which tells a client where to find the authorization server.

The error descriptions are detailed (the exact expiry time, "Invalid signature"). That helps a real
client, but it also confirms to an attacker which check failed. That is acceptable for a bearer API;
lecture 10.7 covers what must not leak.

## Why the HTTP layer, not a resolver

One request can hold many fields. If each resolver checked the token, one would forget, and a bad token
would still get a partial `data` with errors. At the filter, a bad token never reaches GraphQL: no
parsing, no validation cost, and one 401 that every HTTP client and proxy understands. Resolvers get a
verified principal and make authorization decisions only (lab 10.2). The same place also serves
GraphiQL, REST and actuator, so there is one policy rather than one per protocol.

## Sessions or JWTs for Shelfie

Sessions: a cookie, state on the server, instant logout, and CSRF to defend against (lecture 10.3).
JWTs: no lookup per request and any service can verify them offline with the public key (labs 10.2 on
use only `course-public.pem`), but a token stays valid until `exp` and revocation needs a deny list or
short lifetimes. Shelfie's API serves a single-page app, a CLI and later other services (Module 15), so
short-lived JWTs (5 minutes, the realm's `accessTokenLifespan: 300`) with refresh tokens fit. A
browser-only Shelfie could use a session cookie through a backend-for-frontend instead and never expose
the token to JavaScript.

## What a stolen token can do, and for how long

Everything its user can do, until `exp`: 300 seconds here. The API cannot tell a thief from the owner.
Limits: keep the lifetime short, narrow the audience (A07 shows a token for another API is useless here),
keep tokens out of logs and URLs (lecture 10.7), and revoke the refresh token in Keycloak, which stops new
access tokens but not the current one.

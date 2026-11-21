# Authentication: notes

## The experiment: one query, nine tokens

| Token | HTTP status | What the server said (WWW-Authenticate or the GraphQL error) |
|---|---|---|
| none | | |
| Keycloak, ada | | |
| expired | | |
| forged (wrong key) | | |
| alg none | | |
| wrong audience | | |
| wrong issuer | | |

## Why the HTTP layer, not a resolver

## Sessions or JWTs for Shelfie

## What a stolen token can do, and for how long

# Lab 2.6 notes (reference)

## Type counts

Measured 2026-11-21 (counts include built-in scalars, exclude `__` types):

| API | URL | Types (`--count`) | Did the full introspection query work? |
|---|---|---|---|
| Shelfie | http://shelfie-ref:8080/graphql | 14 | yes |
| Countries | https://countries.trevorblades.com/ | 13 | yes |
| SWAPI | https://swapi-graphql.netlify.app/graphql | 58 | no, legacy query needed |
| AniList | https://graphql.anilist.co | 188 | no, legacy query needed (HTTP 400) |
| Rick and Morty | https://rickandmortyapi.com/graphql | 17 | no, legacy query needed |

## What broke, and why

They did not know `__Directive.isRepeatable`, `__Type.isOneOf`, `__InputValue.isDeprecated` and the
`includeDeprecated` argument on `args` and `inputFields`. Those meta-fields arrived in later spec
editions. Introspection is itself versioned by the spec the server implements; tools such as GraphiQL
probe or fall back to an older query shape.

## Should production expose introspection?

For: public APIs (GitHub, Shopify) publish their schema anyway, and tooling, codegen and AI agents
depend on it. Against: on a private API it hands an attacker a map of every field, including ones no
client uses yet. Common middle ground: off in production for private graphs, schema shipped through a
registry instead (Modules 10 and 15).

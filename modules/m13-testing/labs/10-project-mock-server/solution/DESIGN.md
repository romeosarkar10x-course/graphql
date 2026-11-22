# Mock server: design

## Data

Every value is a function of the seed and the response path (`repository/issues/edges/1/node/title`):
deterministic for a seed, different between positions and between seeds, and no state to reset. Field
names and scalar names steer the values (email, url, `*At` and DateTime, `*Count`, rating, name, title,
body), an `id` argument is echoed into the object's `id`, `first`/`last`/`limit` size a list exactly, and
other lists get 2 to 4 items. Abstract types pick one of their possible types by path, so a list of a union
mixes its members. Custom scalars pass through: the fetcher already chose a value that suits the scalar's
name, which is why `URI` comes out as an https URL and `HTML` as text.

## MockSpec, server-side

The RFC (0, strawman) is written for clients: a client sees `@mock` and never sends the request. Reading it
in the server instead lets a Java, Kotlin or curl client use the same `__graphql_mocks__`-style files a
JavaScript client would. What the server does is the RFC's algorithm: the `__default__` entry or the named
one, an error listing the available names, a generated `{"__default__": ...}` when the file is missing, and
the RFC's own validity check (run the operation with no resolvers and the mock's data as the root value).
The server adds `directive @mock(name: String) on QUERY | MUTATION | SUBSCRIPTION` to any schema that lacks
it, so documents with `@mock` validate.

## What it cannot do

It knows types, not meaning: two paths to the same book give two titles, a mutation does not change what a
later query returns, and arguments other than ids and sizes are ignored (`books(titleContains: "sun")`
returns any titles). Named mocks are the escape hatch for the states that matter to a screen: empty,
null, error.

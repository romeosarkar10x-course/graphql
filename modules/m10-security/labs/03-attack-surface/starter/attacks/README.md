# Your attacks

One file per attack, run against the vulnerable fork (`vulnerable.sh start`). Each `.graphql` file is a
single request; `attack.sh <file>` sends it and prints what it cost the server.

| File | The attack | It works when |
|---|---|---|
| `depth.graphql` | deep nesting | depth 20 or more, and the server answers with data |
| `aliases.graphql` | alias overloading | 100+ aliases of one field in one request, answered |
| `fanout.graphql` | list amplification | the server reports 100,000+ field fetches (`extensions.debug`) and survives |
| `cycle.graphql` | fragment cycle | the document contains a cycle (what does the server do?) |
| `suggestions.graphql` | field suggestions | a near miss of a real field (does the server suggest it?) |
| `introspection.graphql` | introspection exposure | the answer names the field the docs don't mention |
| `injection.graphql` | injection in a resolver | the answer contains a row from a table that is not `book` |
| `csrf.hurl` | CSRF with a simple content type | 3+ titled entries (`# C01`...): sign in, a `text/plain` mutation with only the cookie works, and the GET version |
| `brute-force.sh` | batched brute force | prints c1's invite code on its last line, in 12 HTTP requests or fewer |

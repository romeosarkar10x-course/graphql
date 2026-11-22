# Hints: 18.8 Polyglot day

1. Get the all-Java `team.json` (the starter) green first; then swap one subgraph at a time, so a failure has
   one suspect.
2. A subgraph that starts but does not compose usually differs in its SDL: compare its `_service { sdl }` with the
   contract's `.graphqls` (federation version in `@link`, `@key` fields, `@shareable` on `User.handle`, `@provides`).
3. A query that fails only through the router is usually entity resolution: send the router's request yourself,
   `_entities(representations: [{__typename: "User", id: "u1"}])`, straight to the subgraph and compare with the
   Java one's answer.

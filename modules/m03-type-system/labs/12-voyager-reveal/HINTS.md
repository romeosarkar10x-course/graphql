# Hints, game 3c

1. Start from `Query` and walk: every field of an object type is an edge. You have selected most of them in Modules 0 to 2: `club`, `book`, `user`, `search`, `members`, `currentBook`, `reviews`, `author`.
2. Do not forget the types you never selected: `Subscription` (Module 2.5), `Mutation` and its payloads (Module 4 adds them), `Visibility`, `SearchResult`.
3. In Voyager, the left panel's type list is the honest count; the graph hides scalars and can hide root types. `bin/course gql schema-lines http://localhost:8080/graphql/schema` gives you the same facts as text.

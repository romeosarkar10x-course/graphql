# The normalised cache contract (lab 12.5, mini-project 12.9)

Language-neutral: the scenarios in `scenarios/*.json` are the spec. `Scenarios.java` runs them against a Java
`NormalizedCache` class; a cache in another language would need only its own runner.

## Operations

| Operation | Java (`NormalizedCache`) | Lab |
|---|---|---|
| construct with policies | `new NormalizedCache(Map<String, Object> policies)` (empty map: no policies) | 12.5 |
| write a response | `void write(String document, Map<String, Object> variables, Map<String, Object> data)` | 12.5 |
| read a document | `Map<String, Object> read(String document, Map<String, Object> variables)`: the data, or `null` on a miss | 12.5 |
| the store | `Map<String, Object> extract()`: every record, base layer only | 12.5 |
| evict | `void evict(String id)`, `void evict(String id, String fieldKey)` | 12.9 |
| garbage-collect | `List<String> gc()`: removes unreachable records, returns their ids sorted | 12.9 |
| optimistic layers | `void writeOptimistic(String layer, String document, Map<String, Object> variables, Map<String, Object> data)`, `void removeOptimistic(String layer)` | 12.9 |
| watch | `Runnable watch(String document, Map<String, Object> variables, java.util.function.Consumer<Map<String, Object>> listener)`; the `Runnable` cancels | 12.9 |

Data and variables are plain JSON values: `Map`, `List`, `String`, `Integer`/`Long`/`Double`, `Boolean`, `null`.

## The store

- **Cache id.** An object whose selection gave it both `__typename` and `id` is an *entity*, stored once under
  `<__typename>:<id>` (`Book:b1`). With a `keyFields` policy for its type, the id is
  `<__typename>:<JSON object of those fields>` (`Edition:{"isbn":"9780441478125"}`). Objects without an id are
  *embedded*: stored inside their parent, replaced wholesale when written again.
- **Root.** Query root fields live in the record `ROOT_QUERY`. Mutation root fields are not stored, but the
  entities inside a mutation result are, which is how a mutation updates every query that shows them.
- **Field keys.** A field without arguments is stored under its name. With arguments, under
  `name(<JSON object of the arguments>)`: keys sorted, no spaces, variables replaced by their values, an
  argument whose variable is absent left out: `book({"id":"b1"})`, `reviews({"last":2})`. Aliases do not change
  the key; a read gives each field its alias again. A `keyArgs` policy keeps only the listed arguments
  (`keyArgs: []` gives the bare name).
- **References.** An entity inside a record is `{"__ref": "Book:b1"}`; lists hold values, embedded objects or
  references.
- **Merging.** Writing an entity again adds and overwrites fields, never removes them.

## Reading

A read returns the document's shape (aliases as keys) or `null` when **any** selected field is missing.
A stored `null` is a value, not a miss. `__typename` is read from the record like any field.

## Policies (mini-project)

```json
{
  "possibleTypes": { "SearchResult": ["Book", "Club", "User"] },
  "types":  { "Edition": { "keyFields": ["isbn"] } },
  "fields": {
    "Query.feed": { "keyArgs": [], "merge": "append" },
    "Query.book": { "read": "toReference", "typename": "Book", "idArg": "id" }
  }
}
```

- `possibleTypes`: an inline fragment or fragment on an abstract type applies to its member types. A fragment
  on a concrete type applies when it equals the object's `__typename`.
- `keyArgs` with `merge: "append"`: pages written under one key are concatenated in write order; a read
  returns the whole list.
- `read: "toReference"`: when `ROOT_QUERY` has no value for the field, the read follows to
  `<typename>:<value of idArg>` if that record exists (a book cached by a list query is a hit for `book(id:)`).

## Eviction, gc, optimistic layers, watches (mini-project)

- `evict(id)` removes a record; `evict(id, fieldKey)` one field of it. Reads through them then miss.
- `gc()` removes every record not reachable from `ROOT_QUERY` through `__ref`s (in any layer), and returns
  their ids sorted.
- Optimistic layers sit on top of the base store in the order written; a read sees base, then each layer over
  it. `removeOptimistic(layer)` drops that layer only; base writes made while it existed stay.
  `extract()` shows the base only.
- A watch's listener runs after any write, eviction or layer change that changes the watch's read result
  (compared as values), with the new result (`null` when it became a miss). Not when the result is unchanged,
  and never after the watch is cancelled.

## Scenario format

```json
{"description": "...", "policies": {}, "ops": [
  {"op": "write", "query": "...", "variables": {}, "data": {}},
  {"op": "read", "query": "...", "variables": {}, "expect": {} },
  {"op": "extract", "expect": {}},
  {"op": "evict", "id": "Book:b1", "field": "optional field key"},
  {"op": "gc", "expect": ["Book:b9"]},
  {"op": "writeOptimistic", "layer": "l1", "query": "...", "variables": {}, "data": {}},
  {"op": "removeOptimistic", "layer": "l1"},
  {"op": "watch", "name": "w", "query": "...", "variables": {}},
  {"op": "notified", "name": "w", "count": 1, "last": {}},
  {"op": "unwatch", "name": "w"}
]}
```

`"expect": null` means a miss. `notified` checks how many times the listener ran since the watch started and
the value it got last.

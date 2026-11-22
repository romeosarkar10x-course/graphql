# Lab 15.4 notes

## _entities calls with and without batching

I ran R01 (ten reviews, six distinct books) with each form of catalog's `@EntityMapping` and read `/_stats`.
The router sent one `_entities` request to catalog every time, with six representations: it removes
duplicates itself. What changed is the work inside catalog:

| entity method | store queries |
|---|---|
| catalog, single: `@EntityMapping("Book") Book bookEntity(@Argument String id)` | 6 for R01 (`bookById` six times) |
| catalog, List: `List<Book> book(List<Map<String, Object>> representations)` | 1 for R01 (`booksByIdOrIsbn`) |
| reviews, DataLoader: `CompletableFuture<Review> review(@Argument String id, DataLoader<String, Review>)` | 1 for E02's three representations |

So "batching entity lookups" is not about HTTP calls between router and subgraph (always one per subgraph
per plan step) but about what one `_entities` request costs the subgraph's database. The same for fields a
subgraph adds to entities: before the shared `reviewsByBook` DataLoader, R02 cost reviews 15 store queries
(`reviews`, `reviewCount` and `averageRating` for five books); after, one.

## Three forms: when each

- Single: when resolving a representation costs nothing (reviews' `Book` and `User` are only keys) or the
  data is in memory. Spring calls it once per representation.
- List: one call per type with all ids; the natural fit for "WHERE id IN (...)", and the only one that sees
  every representation at once, which handles two keys (`id` or `isbn`) in one query. It must keep order and
  return null where nothing matched.
- DataLoader: when the same lookup is also needed elsewhere in the subgraph, so entity resolution and normal
  fields share one batch and one cache. A mapped batch loader answers missing keys with null; a Flux loader
  with a null element fails the whole batch (I hit that in the probe).

Spring refuses to start when a `@key` type has no `@EntityMapping` ("Unmapped entity types: 'Review'"),
which is a good guard: the router would otherwise plan through a lookup that does not exist.

## The DGS equivalent

DGS writes the same thing as `@DgsEntityFetcher(name = "Book") Book book(Map<String, Object> values)`, one
call per representation, with batching through a DGS DataLoader. Spring's List form has no direct DGS
counterpart; with DGS you return a `CompletableFuture` from a DataLoader to get one query.

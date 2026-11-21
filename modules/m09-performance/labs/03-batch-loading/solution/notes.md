# Batch loading: notes

## Statements for the dashboard

| Version | Statements | Warm latency |
|---|---|---|
| Naive resolvers (lab 9.1) | 1,250 | about 200 ms |
| Batched, without the chaining flag | no response (the dashboard selects ownerPicks) | - |
| All fields batched (@BatchMapping + BatchLoaderRegistry) | 8 | about 50 ms |

```
naive     ████████████████████████████████████████ 1250
batched   ▏ 8
```

## Which fields needed what

`owner`, `currentBook`, `memberCount`, `book` and `author` have no arguments: @BatchMapping, one method
taking all the parents. `members(first)`, `reviews(first)` and `ownerPicks(first)` have arguments, so
the loader key is (parent id, first), registered in BatchLoaderRegistry; the SQL takes the largest
`first` with a window function and each key keeps its own prefix.

## Chained loaders

`ownerPicks` loads the owner's reviews, then their books, in `thenCompose`. graphql-java dispatches the
loaders of a level once; the books load happens after that, so without
`DataLoaderDispatchingContextKeys.setEnableDataLoaderChaining` the request never returns. Spring for
GraphQL 2.1 doesn't set it; an interceptor does. With it, the owner's reviews and the members' reviews
even went out in one batch: 8 statements, not 9.

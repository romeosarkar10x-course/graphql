# Lab 5.2 notes

1. Every scalar line (`name`, `handle`, `rating`, `title`) and `Club.name`: no fetcher was registered, so
   graphql-java uses its default `PropertyDataFetcher`, which reads a record component, getter, public
   field or map key with the field's name from the source. That is also why a Java record with an `ownerId`
   component cannot serve `owner` by itself: the names do not match, so we wrote that fetcher.
2. Neither strictly: each field is fetched and its value completed before the next sibling is fetched when
   the fetchers are synchronous, so `owner` then `handle` (under owner) come before `members`, which is
   depth-first per field. But the lines `[0] handle`, `[0] reviews`, `[1] handle`, `[1] reviews` come before
   any review's `rating`: list items' fields are all fetched before their children are completed. Order is an
   implementation detail; the spec only promises results are assembled as if resolved in field order.
3. Local context flows down one branch only, from parent to its children, so `roleInClub` under `c2`'s
   members sees `c2` and the same user reached via `user(id:)` sees nothing. GraphQLContext is one map
   for the whole request: right for the viewer, tenant or a DataLoader registry, wrong for "where am I in
   the tree", because sibling branches would overwrite each other.
4. `{"errors":[{"message":"No book with id zz","path":["booksById",1],"locations":[...],"extensions":
   {"code":"NOT_FOUND","classification":"DataFetchingException"}}],"data":{"booksById":[{"title":"Piranesi"},null]}}`.
   The client renders the found book, and uses the error's path to mark entry 1 as missing rather than
   failing the whole screen; the list's item type is nullable (`[Book]!`) exactly to allow that.

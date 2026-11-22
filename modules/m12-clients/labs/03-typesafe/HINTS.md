# Hints: m12/03

1. Run `mvn generate-sources` and open `target/generated-sources/dev/shelfie/typed/generated/client/`: there is
   a `ClubGraphQLQuery` (the root field and its arguments) and a `ClubProjectionRoot` (the selection set) per
   root field. `DgsGraphQlClient.create(HttpSyncGraphQlClient.create(RestClient.create(url)))` takes them.
2. A projection method for an object field returns that field's projection, so the chain goes down; `.parent()`
   goes up one level and `.root()` back to the top. `reviews(2)` is `reviews(last: 2)`.
3. Two root fields in one request: `client.request(q1).projection(p1).request(q2).projection(p2).executeSync()`,
   then `response.field("club").toEntity(Club.class)` and `response.field("books").toEntityList(Book.class)`.

# K01 the signed-in reader, from your Kotlin subgraph, through the router (X-Shelfie-User forwarded)
POST {{url}}
X-Shelfie-User: u5
```graphql
{ me { name handle city } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.me.name" == "Elena Petrova"
jsonpath "$.data.me.city" == "Sofia"

# K02 no header, no reader
POST {{url}}
```graphql
{ me { name } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.me" == null

# K03 Java's reviews points at your users: the router asks your subgraph for names and cities (_entities)
POST {{url}}
```graphql
{ latestReviews(first: 3) { author { handle name city } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.latestReviews[0].author.name" == "Asha Rao"
jsonpath "$.data.latestReviews[1].author.city" == "Lisbon"
jsonpath "$.data.latestReviews[2].author.handle" == "chloem"

# K04 Java's clubs too, and your users carry fields other teams add (User.reviews, User.clubs)
POST {{url}}
```graphql
{ club(id: "c2") { owner { name } members { handle city clubs { name } } } user(id: "u12") { name reviews { rating } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.club.owner.name" == "Grace Kim"
jsonpath "$.data.club.members" count == 4
jsonpath "$.data.club.members[3].city" == "Chengdu"
jsonpath "$.data.club.members[0].clubs[0].name" == "Slow Burn Mysteries"
jsonpath "$.data.user.name" == "Liu Wei"
jsonpath "$.data.user.reviews[0].rating" == 4

# K05 the same API as the Java accounts subgraph (two requests: Apollo Router refuses introspection mixed with
# other fields, MIXED_INTROSPECTION)
POST {{url}}
```graphql
{ users { id } }
```
HTTP 200
[Asserts]
jsonpath "$.data.users" count == 12
POST {{url}}
```graphql
{ __type(name: "User") { fields { name } } }
```
HTTP 200
[Asserts]
jsonpath "$.data.__type.fields[*].name" contains "city"
jsonpath "$.data.__type.fields[*].name" contains "reviews"
jsonpath "$.data.__type.fields[*].name" contains "clubs"

# K06 directly: unknown users resolve to null
POST {{accounts}}
```graphql
query($r: [_Any!]!) { _entities(representations: $r) { ... on User { name } } }
variables {"r": [{"__typename": "User", "id": "u1"}, {"__typename": "User", "id": "u404"}]}
```
HTTP 200
[Asserts]
jsonpath "$.data._entities[0].name" == "Asha Rao"
jsonpath "$.data._entities[1]" == null

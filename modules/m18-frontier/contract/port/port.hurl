# Shelfie's reviews subgraph, any language. The contract is Module 15's reviews.graphqls; Java's subgraph passes
# this file unchanged, and so must every port. Variables: reviews (the subgraph's base URL), router (the router's
# GraphQL URL, with Java's accounts, catalog and clubs behind it).

# P01 the subgraph describes itself: Review, Book and User entities keyed by id, the author's handle provided
POST {{reviews}}/_reset
HTTP 200
POST {{reviews}}/graphql
```graphql
{ _service { sdl } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data._service.sdl" matches /specs\.apollo\.dev\/federation\/v2\.[0-9]+/
jsonpath "$.data._service.sdl" matches /type Review[^{]*@key\(fields ?: ?"id"[,)]/
jsonpath "$.data._service.sdl" matches /type Book[^{]*@key\(fields ?: ?"id"[,)]/
jsonpath "$.data._service.sdl" matches /type User[^{]*@key\(fields ?: ?"id"[,)]/
jsonpath "$.data._service.sdl" matches /author: User! @provides\(fields ?: ?"handle"\)/

# P02 one review by id; an unknown id is null, not an error
POST {{reviews}}/graphql
```graphql
{ found: review(id: "r3") { id rating body author { id handle } book { id } } missing: review(id: "r404") { id } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.found.rating" == 5
jsonpath "$.data.found.body" == "The ice crossing chapter. That's it, that's the review."
jsonpath "$.data.found.author.id" == "u3"
jsonpath "$.data.found.author.handle" == "chloem"
jsonpath "$.data.found.book.id" == "b1"
jsonpath "$.data.missing" == null

# P03 latest reviews: newest first, five by default
POST {{reviews}}/graphql
```graphql
{ five: latestReviews { id } two: latestReviews(first: 2) { id rating } none: latestReviews(first: 0) { id } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.five" count == 5
jsonpath "$.data.five[0].id" == "r15"
jsonpath "$.data.five[1].id" == "r14"
jsonpath "$.data.five[2].id" == "r13"
jsonpath "$.data.five[3].id" == "r12"
jsonpath "$.data.five[4].id" == "r11"
jsonpath "$.data.two" count == 2
jsonpath "$.data.two[0].id" == "r15"
jsonpath "$.data.two[1].id" == "r14"
jsonpath "$.data.two[1].rating" == 5
jsonpath "$.data.none" count == 0

# P04 Review entities in the order asked; an unknown one is null, never an INTERNAL_ERROR
POST {{reviews}}/graphql
```graphql
query($r: [_Any!]!) { _entities(representations: $r) { ... on Review { id rating body } } }
variables {
  "r": [
    {"__typename": "Review", "id": "r12"},
    {"__typename": "Review", "id": "r404"},
    {"__typename": "Review", "id": "r6"}
  ]
}
```
HTTP 200
[Asserts]
jsonpath "$.errors[*].extensions.classification" not contains "INTERNAL_ERROR"
jsonpath "$.errors[*].extensions.code" not contains "INTERNAL_SERVER_ERROR"
jsonpath "$.data._entities[0].body" == "Funny and furious."
jsonpath "$.data._entities[1]" == null
jsonpath "$.data._entities[2].rating" == 5

# P05 reviews adds fields to the books it is handed: oldest first, the mean to 2 decimals, null without reviews
POST {{reviews}}/graphql
```graphql
query($r: [_Any!]!) { _entities(representations: $r) { ... on Book { id reviewCount averageRating reviews { id } } } }
variables {
  "r": [
    {"__typename": "Book", "id": "b1"},
    {"__typename": "Book", "id": "b5"},
    {"__typename": "Book", "id": "b2"},
    {"__typename": "Book", "id": "nope"}
  ]
}
```
HTTP 200
[Asserts]
jsonpath "$.errors[*].extensions.classification" not contains "INTERNAL_ERROR"
jsonpath "$.data._entities[0].reviewCount" == 5
jsonpath "$.data._entities[0].averageRating" == 4.2
jsonpath "$.data._entities[0].reviews" count == 5
jsonpath "$.data._entities[0].reviews[0].id" == "r1"
jsonpath "$.data._entities[0].reviews[1].id" == "r2"
jsonpath "$.data._entities[0].reviews[2].id" == "r3"
jsonpath "$.data._entities[0].reviews[3].id" == "r4"
jsonpath "$.data._entities[0].reviews[4].id" == "r5"
jsonpath "$.data._entities[1].reviewCount" == 0
jsonpath "$.data._entities[1].averageRating" == null
jsonpath "$.data._entities[2].averageRating" == 4.5
jsonpath "$.data._entities[3]" == null

# P06 and to the users it is handed: their reviews in id order
POST {{reviews}}/graphql
```graphql
query($r: [_Any!]!) { _entities(representations: $r) { ... on User { id reviews { id book { id } } } } }
variables {
  "r": [
    {"__typename": "User", "id": "u1"},
    {"__typename": "User", "id": "u11"},
    {"__typename": "User", "id": "u404"}
  ]
}
```
HTTP 200
[Asserts]
jsonpath "$.errors[*].extensions.classification" not contains "INTERNAL_ERROR"
jsonpath "$.data._entities[0].reviews" count == 3
jsonpath "$.data._entities[0].reviews[0].id" == "r1"
jsonpath "$.data._entities[0].reviews[1].id" == "r6"
jsonpath "$.data._entities[0].reviews[2].id" == "r15"
jsonpath "$.data._entities[0].reviews[2].book.id" == "b8"
jsonpath "$.data._entities[1].reviews" count == 0
jsonpath "$.data._entities[2]" == null

# P07 addReview without a reader, or with one Shelfie does not know: null and an UNAUTHORIZED error at its path
POST {{reviews}}/graphql
```graphql
mutation { addReview(bookId: "b2", rating: 3, body: "Who am I?") { id } }
```
HTTP 200
[Asserts]
jsonpath "$.data.addReview" == null
jsonpath "$.errors[0].message" == "sign in to review"
jsonpath "$.errors[0].path" count == 1
jsonpath "$.errors[0].path[0]" == "addReview"
jsonpath "$.errors[0].extensions.classification" == "UNAUTHORIZED"
POST {{reviews}}/graphql
X-Shelfie-User: u404
```graphql
mutation { addReview(bookId: "b2", rating: 3, body: "Nobody") { id } }
```
HTTP 200
[Asserts]
jsonpath "$.data.addReview" == null
jsonpath "$.errors[0].extensions.classification" == "UNAUTHORIZED"

# P08 a signed-in reader adds review r16; the book's figures move at once; _reset puts the seed back
POST {{reviews}}/graphql
X-Shelfie-User: u3
```graphql
mutation { addReview(bookId: "b2", rating: 3, body: "Lovely, but the statues stayed with me more than the plot.") { id rating author { id handle } book { id } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.addReview.id" == "r16"
jsonpath "$.data.addReview.author.handle" == "chloem"
jsonpath "$.data.addReview.book.id" == "b2"
POST {{reviews}}/graphql
```graphql
{ latestReviews(first: 1) { id } _entities(representations: [{__typename: "Book", id: "b2"}]) { ... on Book { reviewCount averageRating } } }
```
HTTP 200
[Asserts]
jsonpath "$.data.latestReviews[0].id" == "r16"
jsonpath "$.data._entities[0].reviewCount" == 3
jsonpath "$.data._entities[0].averageRating" == 4.0
POST {{reviews}}/_reset
HTTP 200
POST {{reviews}}/graphql
```graphql
{ latestReviews(first: 1) { id } review(id: "r16") { id } }
```
HTTP 200
[Asserts]
jsonpath "$.data.latestReviews[0].id" == "r15"
jsonpath "$.data.review" == null

# R01 through the router: catalog's title, your bodies and figures, accounts' names in one answer
POST {{router}}
```graphql
{ book(id: "b4") { title averageRating reviewCount reviews { body author { handle name } } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.book.title" == "The Name of the Rose"
jsonpath "$.data.book.averageRating" == 4.5
jsonpath "$.data.book.reviews[0].body" == "Labyrinthine in every sense."
jsonpath "$.data.book.reviews[1].author.handle" == "liuw"
jsonpath "$.data.book.reviews[1].author.name" == "Liu Wei"

# R02 your latest reviews, completed by catalog and accounts
POST {{router}}
```graphql
{ latestReviews(first: 3) { id author { handle name city } book { title year } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.latestReviews" count == 3
jsonpath "$.data.latestReviews[0].id" == "r15"
jsonpath "$.data.latestReviews[1].id" == "r14"
jsonpath "$.data.latestReviews[2].id" == "r13"
jsonpath "$.data.latestReviews[0].author.city" == "Bengaluru"
jsonpath "$.data.latestReviews[0].book.title" == "Station Eleven"
jsonpath "$.data.latestReviews[1].book.year" == 1989

# R03 clubs' members and current books, with your ratings and reviews of them
POST {{router}}
```graphql
{ clubs { name currentBook { title weeksToRead averageRating } owner { handle reviews { id } } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.clubs[0].currentBook.averageRating" == 4.2
jsonpath "$.data.clubs[0].currentBook.weeksToRead" == 4
jsonpath "$.data.clubs[1].owner.reviews" count == 1
jsonpath "$.data.clubs[1].owner.reviews[0].id" == "r8"
jsonpath "$.data.clubs[2].currentBook.averageRating" == 4.5

# R04 the router forwards the reader to you: the mutation works through it, and the graph sees the new review
POST {{router}}
X-Shelfie-User: u9
```graphql
mutation { addReview(bookId: "b8", rating: 5, body: "Read it on a train, missed my stop.") { id author { handle name } book { title reviewCount } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.addReview.id" == "r16"
jsonpath "$.data.addReview.author.name" == "Ines Duarte"
jsonpath "$.data.addReview.book.title" == "Station Eleven"
jsonpath "$.data.addReview.book.reviewCount" == 2
POST {{router}}
```graphql
mutation { addReview(bookId: "b8", rating: 1, body: "Anonymous") { id } }
```
HTTP 200
[Asserts]
jsonpath "$.data.addReview" == null
jsonpath "$.errors[0].message" == "sign in to review"
POST {{reviews}}/_reset
HTTP 200

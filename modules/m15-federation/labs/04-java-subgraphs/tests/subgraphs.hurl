# S01 catalog describes itself as a federated subgraph, with both of Book's keys
POST {{catalog}}/graphql
```graphql
{ _service { sdl } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data._service.sdl" matches /type Book[^{]*@key\(fields ?: ?"id"/
jsonpath "$.data._service.sdl" matches /type Book[^{]*@key\(fields ?: ?"isbn"/

# S02 reviews too
POST {{reviews}}/graphql
```graphql
{ _service { sdl } }
```
HTTP 200
[Asserts]
jsonpath "$.data._service.sdl" matches /@provides\(fields ?: ?"handle"\)/

# E01 catalog resolves Book representations by id or by isbn, unknown ones as null, in one store query
POST {{catalog}}/_reset
HTTP 200
POST {{catalog}}/graphql
```graphql
query($r: [_Any!]!) { _entities(representations: $r) { ... on Book { id title } } }
variables {
  "r": [
    {"__typename": "Book", "id": "b4"},
    {"__typename": "Book", "isbn": "9781635575637"},
    {"__typename": "Book", "id": "b99"},
    {"__typename": "Book", "id": "b1"}
  ]
}
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data._entities[0].title" == "The Name of the Rose"
jsonpath "$.data._entities[1].id" == "b2"
jsonpath "$.data._entities[2]" == null
jsonpath "$.data._entities[3].title" == "The Left Hand of Darkness"
GET {{catalog}}/_stats
HTTP 200
[Asserts]
jsonpath "$.queries" == 1

# E02 reviews resolves Review representations in one store query, unknown ones as null (never an INTERNAL_ERROR)
POST {{reviews}}/_reset
HTTP 200
POST {{reviews}}/graphql
```graphql
query($r: [_Any!]!) { _entities(representations: $r) { ... on Review { id rating body } } }
variables {
  "r": [
    {"__typename": "Review", "id": "r3"},
    {"__typename": "Review", "id": "r404"},
    {"__typename": "Review", "id": "r12"}
  ]
}
```
HTTP 200
[Asserts]
jsonpath "$.errors[*].extensions.classification" not contains "INTERNAL_ERROR"
jsonpath "$.data._entities[0].body" == "The ice crossing chapter. That's it, that's the review."
jsonpath "$.data._entities[1]" == null
jsonpath "$.data._entities[2].rating" == 5
GET {{reviews}}/_stats
HTTP 200
[Asserts]
jsonpath "$.queries" == 1

# E03 reviews adds fields to books it is handed: three fields, three books, one store query
POST {{reviews}}/_reset
HTTP 200
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
jsonpath "$.data._entities[0].reviews[0].id" == "r1"
jsonpath "$.data._entities[1].reviewCount" == 0
jsonpath "$.data._entities[1].averageRating" == null
jsonpath "$.data._entities[2].averageRating" == 4.5
jsonpath "$.data._entities[3]" == null
GET {{reviews}}/_stats
HTTP 200
[Asserts]
jsonpath "$.queries" == 1

# R01 through the router: ten reviews, six distinct books, one catalog store query
POST {{catalog}}/_reset
HTTP 200
POST {{router}}
```graphql
{ latestReviews(first: 10) { id author { handle } book { title isbn } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.latestReviews" count == 10
jsonpath "$.data.latestReviews[0].book.title" == "Station Eleven"
jsonpath "$.data.latestReviews[0].author.handle" == "asha"
jsonpath "$.data.latestReviews[9].book.isbn" == "9781635575637"
GET {{catalog}}/_stats
HTTP 200
[Asserts]
jsonpath "$.queries" == 1

# R02 five books from catalog, their reviews and figures from reviews: one reviews store query
POST {{reviews}}/_reset
HTTP 200
POST {{router}}
```graphql
{ books(first: 5) { title reviewCount averageRating reviews { rating author { handle } } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.books[0].reviewCount" == 5
jsonpath "$.data.books[0].reviews[3].author.handle" == "jonasb"
jsonpath "$.data.books[3].averageRating" == 4.5
jsonpath "$.data.books[4].reviews" count == 0
GET {{reviews}}/_stats
HTTP 200
[Asserts]
jsonpath "$.queries" == 1

# R03 three subgraphs in one answer: catalog's title, reviews' bodies, accounts' names
POST {{router}}
```graphql
{ book(id: "b4") { title averageRating reviews { body author { handle name } } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.book.reviews[0].body" == "Labyrinthine in every sense."
jsonpath "$.data.book.reviews[1].author.name" == "Liu Wei"

# R04 and clubs: weeksToRead needs your catalog's pages (@requires), the rating needs your reviews
POST {{router}}
```graphql
{ club(id: "c2") { name currentBook { title weeksToRead averageRating } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.club.currentBook.weeksToRead" == 6
jsonpath "$.data.club.currentBook.averageRating" == 4.5

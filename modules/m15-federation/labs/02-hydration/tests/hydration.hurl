# N01 catalog's fields through the gateway
POST {{url}}
```graphql
{ book(id: "b2") { title authors pages } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.book.title" == "Piranesi"
jsonpath "$.data.book.pages" == 272

# N02 a review's book is hydrated from catalog: five reviews, one call to each service
POST {{catalog}}/_reset
HTTP 200
POST {{reviews}}/_reset
HTTP 200
POST {{url}}
```graphql
{ latestReviews(first: 5) { id rating book { title } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.latestReviews" count == 5
jsonpath "$.data.latestReviews[0].book.title" == "Station Eleven"
jsonpath "$.data.latestReviews[2].book.title" == "The Seven Moons of Maali Almeida"
jsonpath "$.data.latestReviews[4].book.title" == "Project Hail Mary"
GET {{catalog}}/_stats
HTTP 200
[Asserts]
jsonpath "$.requests" == 1
GET {{reviews}}/_stats
HTTP 200
[Asserts]
jsonpath "$.requests" == 1

# N03 the join keys stay inside the gateway: clients never see bookId or authorId
POST {{url}}
```graphql
{ __type(name: "Review") { fields { name } } }
```
HTTP 200
[Asserts]
jsonpath "$.data.__type.fields[*].name" contains "book"
jsonpath "$.data.__type.fields[*].name" not contains "bookId"
jsonpath "$.data.__type.fields[*].name" not contains "authorId"

# N04 one review and its book
POST {{url}}
```graphql
{ review(id: "r8") { body book { title pages } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.review.body" == "Labyrinthine in every sense."
jsonpath "$.data.review.book.pages" == 536

# N05 an unknown review is null, not an error
POST {{url}}
```graphql
{ review(id: "r99") { id book { title } } }
```
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.review" == null

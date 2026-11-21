# Lab 10.4: Shelfie's real screens. Every one must still work under the limits.

# R01 Home page (anonymous): clubs with members and recent reviews
POST {{url}}
Content-Type: application/json
{"query": "query Home { clubs(first: 10) { id name members(first: 10) { name } reviews(first: 5) { rating book { title } } } }"}
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.clubs" count == 10

# R02 Club page (ada, member of c1)
POST {{url}}
Authorization: Bearer {{ada}}
Content-Type: application/json
{"query": "query Club { club(id: \"c1\") { name private owner { name } members(first: 20) { id name } reviews(first: 20) { id rating body author { name } book { id title } } } }"}
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.club.reviews" count == 20

# R03 Book page with three recommendations (one slow call, 300 ms)
POST {{url}}
Content-Type: application/json
{"query": "query Book { book(id: \"b7\") { title reviews(first: 10) { rating body author { name } club { name } } recommendations { id title } } }"}
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.book.recommendations" count == 3

# R04 Profile (ben): my clubs and their latest reviews
POST {{url}}
Authorization: Bearer {{ben}}
Content-Type: application/json
{"query": "query Me { viewer { name email clubs(first: 10) { name reviews(first: 5) { body book { title } } } } }"}
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.viewer.clubs" count == 2

# R05 Moderation (max): every reader on one page
POST {{url}}
Authorization: Bearer {{max}}
Content-Type: application/json
{"query": "query Readers { readers(first: 40) { id name roles } }"}
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.readers" count == 40

# R06 Catalogue: 50 books, the largest page allowed
POST {{url}}
Content-Type: application/json
{"query": "query Catalogue { books(first: 50) { id title } }"}
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.books" count == 50

# R07 Every response reports its cost
POST {{url}}
Authorization: Bearer {{olu}}
Content-Type: application/json
{"query": "{ books(first: 10) { title } }"}
HTTP 200
[Asserts]
jsonpath "$.extensions.cost.requested" == 20
jsonpath "$.extensions.cost.limit" == 10000
jsonpath "$.extensions.cost.remaining" == 9980

# R08 A query that costs nothing (health checks and scanners send it) still works
POST {{url}}
Authorization: Bearer {{ada}}
Content-Type: application/json
{"query": "{ __typename }"}
HTTP 200
[Asserts]
jsonpath "$.data.__typename" == "Query"

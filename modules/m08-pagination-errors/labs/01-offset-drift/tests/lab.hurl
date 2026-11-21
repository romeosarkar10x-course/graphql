# Lab 8.1 checks. The catalogue (m08_01, 10,000 books) is reloaded before the run; newest first means id 10000 first.

# O01 Defaults: page 0, size 20
POST {{url}}
Content-Type: application/json
{"query": "{ books { items { id } page size totalCount totalPages hasNextPage } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books.items" count == 20
jsonpath "$.data.books.items[0].id" == "10000"
jsonpath "$.data.books.items[19].id" == "9981"
jsonpath "$.data.books.totalCount" == 10000
jsonpath "$.data.books.totalPages" == 500
jsonpath "$.data.books.hasNextPage" == true

# O02 page 2 of size 5 starts at offset 10
POST {{url}}
Content-Type: application/json
{"query": "{ books(page: 2, size: 5) { items { id } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books.items[0].id" == "9990"
jsonpath "$.data.books.items[4].id" == "9986"

# O03 The last page
POST {{url}}
Content-Type: application/json
{"query": "{ books(page: 499) { items { id } hasNextPage } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books.items[0].id" == "20"
jsonpath "$.data.books.items[19].id" == "1"
jsonpath "$.data.books.hasNextPage" == false

# O04 Past the end: empty, not an error
POST {{url}}
Content-Type: application/json
{"query": "{ books(page: 500) { items { id } hasNextPage } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books.items" count == 0
jsonpath "$.data.books.hasNextPage" == false
jsonpath "$.errors" not exists

# O05 size above 50
POST {{url}}
Content-Type: application/json
{"query": "{ books(size: 51) { items { id } } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "BAD_REQUEST"

# O06 size 0
POST {{url}}
Content-Type: application/json
{"query": "{ books(size: 0) { items { id } } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "BAD_REQUEST"

# O07 a negative page
POST {{url}}
Content-Type: application/json
{"query": "{ books(page: -1) { items { id } } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "BAD_REQUEST"

# O08 addBook lands on top of page 0
POST {{url}}
Content-Type: application/json
{"query": "mutation { addBook(title: \"Hurl Was Here\") { id title } }"}
HTTP 200
[Captures]
new_id: jsonpath "$.data.addBook.id"
[Asserts]
jsonpath "$.data.addBook.title" == "Hurl Was Here"

POST {{url}}
Content-Type: application/json
{"query": "{ books(size: 1) { items { id } totalCount } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books.items[0].id" == {{new_id}}
jsonpath "$.data.books.totalCount" == 10001

# O09 deleteBook: true once, then false
POST {{url}}
Content-Type: application/json
{"query": "mutation { a: deleteBook(id: \"{{new_id}}\") }"}
HTTP 200
[Asserts]
jsonpath "$.data.a" == true

POST {{url}}
Content-Type: application/json
{"query": "mutation { b: deleteBook(id: \"{{new_id}}\") }"}
HTTP 200
[Asserts]
jsonpath "$.data.b" == false

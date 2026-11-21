# Lab 6.5 checks.

# D01 Generated: book(id:) through findById
POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b2\") { title author publishedYear } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.title" == "Piranesi"
jsonpath "$.data.book.author" == "Susanna Clarke"

# D02 Generated: Query by Example on one property
POST {{url}}
Content-Type: application/json
{"query": "{ books(publishedYear: 2021) { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books" count == 2

# D03 Generated: two properties, both must match
POST {{url}}
Content-Type: application/json
{"query": "{ a: books(author: \"Kazuo Ishiguro\") { id } b: books(author: \"Kazuo Ishiguro\", publishedYear: 2021) { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.a" count == 2
jsonpath "$.data.b" count == 1
jsonpath "$.data.b[0].id" == "b10"

# D04 Generated: no arguments, an empty example, everything
POST {{url}}
Content-Type: application/json
{"query": "{ books { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books" count == 10

# D05 Hand-written: case-insensitive contains, ordered by year
POST {{url}}
Content-Type: application/json
{"query": "{ searchBooks(titleContains: \"THE\") { id publishedYear } }"}
HTTP 200
[Asserts]
jsonpath "$.data.searchBooks" count == 6
jsonpath "$.data.searchBooks[0].id" == "b1"
jsonpath "$.data.searchBooks[5].id" == "b9"

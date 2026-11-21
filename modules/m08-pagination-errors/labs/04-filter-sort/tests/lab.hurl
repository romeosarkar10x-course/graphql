# Lab 8.4 examples (the oracle in FilterOracle.java checks 300 more). Catalogue m08_04, 10,000 books.

# F01 Default order: title, then id. 460 titles for 10,000 books, so the tie-breaker decides most pages.
POST {{url}}
Content-Type: application/json
{"query": "{ books(first: 3) { items { id title } totalCount } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books.items[0].id" == "382"
jsonpath "$.data.books.items[1].id" == "842"
jsonpath "$.data.books.items[2].id" == "1302"
jsonpath "$.data.books.items[2].title" == "Broken Archive"
jsonpath "$.data.books.totalCount" == 10000

# F02 Every field narrows; RATING DESC ties broken by id ascending
POST {{url}}
Content-Type: application/json
{"query": "{ books(filter: {genres: [POETRY, MEMOIR], publishedFrom: 1990, publishedTo: 1999, minRating: 4.5}, orderBy: {field: RATING, direction: DESC}, first: 3) { items { id genre published rating } totalCount } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books.totalCount" == 30
jsonpath "$.data.books.items[0].id" == "2195"
jsonpath "$.data.books.items[1].id" == "6131"
jsonpath "$.data.books.items[2].genre" == "POETRY"

# F03 Full-text search, most relevant first
POST {{url}}
Content-Type: application/json
{"query": "{ books(filter: {search: \"dragon lisbon\"}, orderBy: {field: RELEVANCE}, first: 3) { items { id } totalCount } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books.totalCount" == 69
jsonpath "$.data.books.items[0].id" == "138"

# F04 Web-search syntax: a quoted phrase and an excluded word
POST {{url}}
Content-Type: application/json
{"query": "{ books(filter: {search: \"\\\"stolen map\\\" -accra\"}, first: 2) { items { id title } totalCount } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books.totalCount" == 700
jsonpath "$.data.books.items[0].id" == "3602"

# F05 An exact author, newest first
POST {{url}}
Content-Type: application/json
{"query": "{ books(filter: {author: \"Mei Mensah\"}, orderBy: {field: PUBLISHED, direction: DESC}, first: 2) { items { id published } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books.items[0].id" == "3554"
jsonpath "$.data.books.items[0].published" == 2023

# F06 A word that matches nothing (the test also checks that it never reaches the SQL text)
POST {{url}}
Content-Type: application/json
{"query": "{ books(filter: {search: \"quixotic\"}) { items { id } totalCount } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books.totalCount" == 0
jsonpath "$.errors" not exists

# F07 An injection attempt is just a strange search
POST {{url}}
Content-Type: application/json
{"query": "{ books(filter: {search: \"x') or true or ('x\"}) { totalCount } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books.totalCount" == 0

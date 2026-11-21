# Lab 9.4: the selection set decides the SQL.

# L01 The whole dashboard in one statement
POST {{url}}
Content-Type: application/json
file,dashboard.json;
HTTP 200
[Asserts]
jsonpath "$.data.clubs" count == 10
jsonpath "$.extensions.sql.statements" == 1

# L02 Ask for less, load less: still one statement
POST {{url}}
Content-Type: application/json
{"query": "{ clubs(first: 3) { name } }"}
HTTP 200
[Asserts]
jsonpath "$.data.clubs[2].name" == "Hollow River Club"
jsonpath "$.extensions.sql.statements" == 1

# L03 Arguments come from the selection: 2 members of 3 clubs, 1 review each
POST {{url}}
Content-Type: application/json
{"query": "{ clubs(first: 3) { members(first: 2) { handle reviews(first: 1) { rating } } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.clubs[0].members" count == 2
jsonpath "$.data.clubs[0].members[0].reviews" count == 1
jsonpath "$.extensions.sql.statements" == 1

# L04 The same list twice with different arguments (aliases) is still right
POST {{url}}
Content-Type: application/json
{"query": "{ clubs(first: 2) { a: members(first: 1) { handle } b: members(first: 3) { handle } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.clubs[0].a" count == 1
jsonpath "$.data.clubs[0].b" count == 3
jsonpath "$.data.clubs[0].b[2].handle" == "reader212"

# L05 A field the lookahead doesn't load (Review.author) still resolves, through its loader
POST {{url}}
Content-Type: application/json
{"query": "{ clubs(first: 2) { members(first: 2) { reviews(first: 1) { author { handle } } } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.clubs[0].members[0].reviews[0].author.handle" == "reader54"
jsonpath "$.extensions.sql.statements" <= 2

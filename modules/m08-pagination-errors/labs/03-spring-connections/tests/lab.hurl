# Lab 8.3 checks. Expected ids come from the course catalogue (bin/course catalog m08_03), ordered by (published, id).

# C01 No arguments: the first 20, hasNextPage
POST {{url}}
Content-Type: application/json
{"query": "{ books { edges { node { id } } pageInfo { hasNextPage hasPreviousPage } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books.edges" count == 20
jsonpath "$.data.books.edges[0].node.id" == "125"
jsonpath "$.data.books.edges[19].node.id" == "2500"
jsonpath "$.data.books.pageInfo.hasNextPage" == true
jsonpath "$.data.books.pageInfo.hasPreviousPage" == false
jsonpath "$.errors" not exists

# C02 first: 3, then the next 4 after its endCursor
POST {{url}}
Content-Type: application/json
{"query": "{ books(first: 3) { edges { cursor node { id } } pageInfo { endCursor } } }"}
HTTP 200
[Captures]
end3: jsonpath "$.data.books.pageInfo.endCursor"
[Asserts]
jsonpath "$.data.books.edges[2].node.id" == "375"
jsonpath "$.data.books.edges[2].cursor" == {{end3}}

# C03 after: no gap, no duplicate
POST {{url}}
Content-Type: application/json
{"query": "{ books(first: 4, after: \"{{end3}}\") { edges { node { id published } } pageInfo { hasNextPage } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books.edges[*].node.id" includes "500"
jsonpath "$.data.books.edges[0].node.id" == "500"
jsonpath "$.data.books.edges[3].node.id" == "875"
jsonpath "$.data.books.pageInfo.hasNextPage" == true

# C04 Backward: the 2 before the third book, in ascending order
POST {{url}}
Content-Type: application/json
{"query": "{ books(last: 2, before: \"{{end3}}\") { edges { node { id } } pageInfo { hasPreviousPage } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books.edges" count == 2
jsonpath "$.data.books.edges[0].node.id" == "125"
jsonpath "$.data.books.edges[1].node.id" == "250"
jsonpath "$.data.books.pageInfo.hasPreviousPage" == false

# C05 last: 3 without a cursor is the end of the list
POST {{url}}
Content-Type: application/json
{"query": "{ books(last: 3) { edges { cursor node { id published } } pageInfo { hasPreviousPage } } }"}
HTTP 200
[Captures]
second_last: jsonpath "$.data.books.edges[1].cursor"
[Asserts]
jsonpath "$.data.books.edges[0].node.id" == "9652"
jsonpath "$.data.books.edges[2].node.id" == "9902"
jsonpath "$.data.books.edges[2].node.published" == 2024
jsonpath "$.data.books.pageInfo.hasPreviousPage" == true

# C06 After the second-to-last book: one left, hasNextPage false
POST {{url}}
Content-Type: application/json
{"query": "{ books(first: 5, after: \"{{second_last}}\") { edges { node { id } } pageInfo { hasNextPage } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books.edges" count == 1
jsonpath "$.data.books.edges[0].node.id" == "9902"
jsonpath "$.data.books.pageInfo.hasNextPage" == false

# C07 first: 0 is allowed and empty
POST {{url}}
Content-Type: application/json
{"query": "{ books(first: 0) { edges { cursor } pageInfo { hasNextPage } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books.edges" count == 0
jsonpath "$.errors" not exists

# C08 first: 101 is over the cap: BAD_REQUEST, not 101 rows
POST {{url}}
Content-Type: application/json
{"query": "{ books(first: 101) { edges { node { id } } } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "BAD_REQUEST"
jsonpath "$.errors[0].path[0]" == "books"

# C09 last: 500 too
POST {{url}}
Content-Type: application/json
{"query": "{ books(last: 500) { edges { node { id } } } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "BAD_REQUEST"

# C10 first: -1 is an error in the Relay spec, not "use the default"
POST {{url}}
Content-Type: application/json
{"query": "{ books(first: -1) { edges { node { id } } } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "BAD_REQUEST"

# C11 first with before: ambiguous, so an error rather than silently ignoring before
POST {{url}}
Content-Type: application/json
{"query": "{ books(first: 2, before: \"{{end3}}\") { edges { node { id } } } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "BAD_REQUEST"

# C12 A cursor we never issued (base64 of "nope"): BAD_REQUEST, not INTERNAL_ERROR
POST {{url}}
Content-Type: application/json
{"query": "{ books(after: \"bm9wZQ\") { edges { node { id } } } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "BAD_REQUEST"
jsonpath "$.errors[0].message" not contains "Exception"

# C13 Not even base64
POST {{url}}
Content-Type: application/json
{"query": "{ books(after: \"%%%\") { edges { node { id } } } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "BAD_REQUEST"

# C14 The connection types are generated: BookEdge has node and cursor
POST {{url}}
Content-Type: application/json
{"query": "{ __type(name: \"BookEdge\") { fields { name } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.__type.fields[*].name" includes "node"
jsonpath "$.data.__type.fields[*].name" includes "cursor"

# Lab 9.1: the app reports how many SQL statements each request ran, in extensions.sql.statements.
# Expected counts come from the naive resolvers in the starter, on bin/course clubs data.

# C01 The club dashboard (contract/dashboard.graphql)
POST {{url}}
Content-Type: application/json
file,dashboard.json;
HTTP 200
[Asserts]
jsonpath "$.data.clubs" count == 10
jsonpath "$.extensions.sql.statements" == 1250

# C02 One club, its owner, three members
POST {{url}}
Content-Type: application/json
{"query": "{ club(id: 1) { name owner { name } members(first: 3) { handle } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.members" count == 3
jsonpath "$.extensions.sql.statements" == 3

# C03 Three levels of lists: 3 clubs, 2 members each, 2 reviews each, and each review's book
POST {{url}}
Content-Type: application/json
{"query": "{ clubs(first: 3) { members(first: 2) { reviews(first: 2) { book { title } } } } }"}
HTTP 200
[Asserts]
jsonpath "$.extensions.sql.statements" == 22

# C04 Nothing to load: no statements
POST {{url}}
Content-Type: application/json
{"query": "{ viewer { handle } }"}
HTTP 200
[Asserts]
jsonpath "$.data.viewer" == null
jsonpath "$.extensions.sql.statements" == 0

# C05 Every request counts from zero
POST {{url}}
Content-Type: application/json
{"query": "{ club(id: 10) { currentBook { title } memberCount } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.currentBook" == null
jsonpath "$.extensions.sql.statements" == 2

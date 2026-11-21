# Lab 9.3: one statement per field per level, whatever the fan-out.

# B01 The dashboard: from 1,250 statements to at most 9
POST {{url}}
Content-Type: application/json
file,dashboard.json;
HTTP 200
[Asserts]
jsonpath "$.data.clubs" count == 10
jsonpath "$.extensions.sql.statements" <= 9

# B02 Three levels of lists: clubs, members, reviews, books
POST {{url}}
Content-Type: application/json
{"query": "{ clubs(first: 3) { members(first: 2) { reviews(first: 2) { book { title } } } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.clubs[2].members[1].reviews" count == 2
jsonpath "$.extensions.sql.statements" <= 4

# B03 The key includes the argument: two different `first` values in one request, both right
POST {{url}}
Content-Type: application/json
{"query": "{ club(id: 1) { two: members(first: 2) { handle } five: members(first: 5) { handle } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.two" count == 2
jsonpath "$.data.club.five" count == 5
jsonpath "$.data.club.five[1].handle" == "reader133"

# B04 A chained loader (the owner's reviews, then their books) answers, in one batch per loader
POST {{url}}
Content-Type: application/json
{"query": "{ clubs(first: 5) { ownerPicks(first: 2) { title } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.clubs[0].ownerPicks" count == 2
jsonpath "$.extensions.sql.statements" <= 3

# B05 Clubs without a current book are still null, and counts are still right
POST {{url}}
Content-Type: application/json
{"query": "{ clubs(first: 10) { currentBook { title } memberCount } }"}
HTTP 200
[Asserts]
jsonpath "$.data.clubs[9].currentBook" == null
jsonpath "$.data.clubs[9].memberCount" == 25
jsonpath "$.extensions.sql.statements" <= 3

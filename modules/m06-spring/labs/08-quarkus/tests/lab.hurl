# Lab 6.8 checks: Shelfie on Quarkus, schema generated from Java.

# Q01 The generated schema uses Shelfie's types: ids are ID!, names non-null
GET {{base}}/graphql/schema.graphql
HTTP 200
[Asserts]
body contains "club(id: ID!): Club"
body contains "clubs: [Club!]!"
body matches /type Club \{[^}]*id: ID!/
body matches /type Club \{[^}]*name: String!/
body matches /type Club \{[^}]*owner: User!/
body matches /type Book \{[^}]*authors: \[String!\]!/
body not contains "ownerId"

# Q02 The club card, nested through @Source
POST {{url}}
Content-Type: application/json
{"query": "{ club(id: \"c1\") { name memberCount owner { handle } currentBook { title averageRating } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.owner.handle" == "kemia"
jsonpath "$.data.club.currentBook.averageRating" == 4.2

# Q03 Three clubs, three current books
POST {{url}}
Content-Type: application/json
{"query": "{ clubs { currentBook { id } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.clubs" count == 3
jsonpath "$.data.clubs[1].currentBook.id" == "b4"

# Q04 ...fetched in one batch call (Q02 made one, Q03 made one)
POST {{url}}
Content-Type: application/json
{"query": "{ currentBookBatches }"}
HTTP 200
[Asserts]
jsonpath "$.data.currentBookBatches" == 2

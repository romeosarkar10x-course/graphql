# Lab 6.7 checks: the DGS slice of Shelfie.

# G01 The club card
POST {{url}}
Content-Type: application/json
{"query": "{ club(id: \"c1\") { name memberCount owner { name handle } currentBook { title averageRating } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.name" == "The Midnight Readers"
jsonpath "$.data.club.memberCount" == 8
jsonpath "$.data.club.owner.handle" == "kemia"
jsonpath "$.data.club.currentBook.averageRating" == 4.2

# G02 All clubs, each with its current book
POST {{url}}
Content-Type: application/json
{"query": "{ clubs { id currentBook { id } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.clubs" count == 3
jsonpath "$.data.clubs[2].currentBook.id" == "b9"

# G03 A book nobody reviewed, and an unknown club
POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b7\") { title averageRating } club(id: \"zz\") { name } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.title" == "The Dispossessed"
jsonpath "$.data.book.averageRating" == null
jsonpath "$.data.club" == null

# Lab 6.2 checks. Run after a fresh start: R03-R05 change club c2 in this order.

# R01 roleInClub: from local context under owner and members, null elsewhere
POST {{url}}
Content-Type: application/json
{"query": "{ club(id: \"c2\") { owner { handle roleInClub } members(first: 2) { handle roleInClub } } user(id: \"u7\") { roleInClub } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.owner.roleInClub" == "OWNER"
jsonpath "$.data.club.members[0].roleInClub" == "OWNER"
jsonpath "$.data.club.members[1].roleInClub" == "MEMBER"
jsonpath "$.data.user.roleInClub" == null
jsonpath "$.errors" not exists

# R02 bookSearch gives the same answer as books
POST {{url}}
Content-Type: application/json
{"query": "{ a: books(titleContains: \"the\", first: 3) { id } b: bookSearch(titleContains: \"the\", first: 3) { id } c: bookSearch { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.b" count == 3
jsonpath "$.data.b[2].id" == "b4"
jsonpath "$.data.c" count == 10

# R03 updateClub with name only: description untouched
POST {{url}}
Content-Type: application/json
{"query": "mutation { updateClub(input: {id: \"c2\", name: \"Slower Burn Mysteries\"}) { name description } }"}
HTTP 200
[Asserts]
jsonpath "$.data.updateClub.name" == "Slower Burn Mysteries"
jsonpath "$.data.updateClub.description" == "Whodunits read at a civilised pace."

# R04 description: null clears it; name left out stays
POST {{url}}
Content-Type: application/json
{"query": "mutation { updateClub(input: {id: \"c2\", description: null}) { name description } }"}
HTTP 200
[Asserts]
jsonpath "$.data.updateClub.name" == "Slower Burn Mysteries"
jsonpath "$.data.updateClub.description" == null

# R05 The same through variables: an omitted key keeps, the edit is visible to queries
POST {{url}}
Content-Type: application/json
{"query": "mutation U($in: UpdateClubInput!) { updateClub(input: $in) { description } } ", "variables": {"in": {"id": "c2", "description": "Back again"}}}
HTTP 200
[Asserts]
jsonpath "$.data.updateClub.description" == "Back again"

POST {{url}}
Content-Type: application/json
{"query": "{ club(id: \"c2\") { name description } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.name" == "Slower Burn Mysteries"
jsonpath "$.data.club.description" == "Back again"

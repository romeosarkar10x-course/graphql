# Lab 10.6: keys, scopes and a budget per client app.

# P01 No API key: 401 before GraphQL runs
POST {{url}}
Content-Type: application/json
{"query": "{ books(first: 1) { id } }"}
HTTP 401

# P02 An unknown key: 401
POST {{url}}
X-Api-Key: pk_guess_9
Content-Type: application/json
{"query": "{ books(first: 1) { id } }"}
HTTP 401

# P03 A reader app: data, and the cost in extensions
POST {{url}}
X-Api-Key: pk_reader_1
Content-Type: application/json
{"query": "{ books(first: 10) { title } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books" count == 10
jsonpath "$.extensions.cost.requestedQueryCost" == 20
jsonpath "$.extensions.cost.throttleStatus.maximumAvailable" == 1000
jsonpath "$.extensions.cost.throttleStatus.currentlyAvailable" <= 980
jsonpath "$.extensions.cost.throttleStatus.restoreRate" == 50

# P04 The reader app writes for ada: the key lacks write:reviews
POST {{url}}
X-Api-Key: pk_reader_1
Authorization: Bearer {{ada}}
Content-Type: application/json
{"query": "mutation { addReview(clubId: \"c2\", bookId: \"b1\", rating: 5, body: \"via app\") { id } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "FORBIDDEN"
jsonpath "$.errors[0].message" contains "write:reviews"

# P05 The review bot may
POST {{url}}
X-Api-Key: pk_writer_2
Authorization: Bearer {{ada}}
Content-Type: application/json
{"query": "mutation { addReview(clubId: \"c2\", bookId: \"b1\", rating: 5, body: \"via bot\") { author { name } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.addReview.author.name" == "Ada"

# P06 Scopes and user rules both apply: the bot writing as ben to c1 is still FORBIDDEN by lab 10.2's rule
POST {{url}}
X-Api-Key: pk_writer_2
Authorization: Bearer {{ben}}
Content-Type: application/json
{"query": "mutation { addReview(clubId: \"c1\", bookId: \"b1\", rating: 1, body: \"x\") { id } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "FORBIDDEN"

# P07 The bot cannot rename clubs, even for their owner
POST {{url}}
X-Api-Key: pk_writer_2
Authorization: Bearer {{olu}}
Content-Type: application/json
{"query": "mutation { renameClub(id: \"c1\", name: \"Bot Owls\") { name } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].message" contains "write:clubs"

# P08 The club tool can
POST {{url}}
X-Api-Key: pk_admin_4
Authorization: Bearer {{olu}}
Content-Type: application/json
{"query": "mutation { renameClub(id: \"c1\", name: \"Night Owls\") { name } }"}
HTTP 200
[Asserts]
jsonpath "$.data.renameClub.name" == "Night Owls"

# P09 The club tool spends 430 twice; the third Home page is THROTTLED, with the numbers to back off by
POST {{url}}
X-Api-Key: pk_admin_4
Content-Type: application/json
{"query": "{ clubs(first: 10) { id name members(first: 10) { name } reviews(first: 5) { rating book { title } } } }"}
HTTP 200

POST {{url}}
X-Api-Key: pk_admin_4
Content-Type: application/json
{"query": "{ clubs(first: 10) { id name members(first: 10) { name } reviews(first: 5) { rating book { title } } } }"}
HTTP 200

POST {{url}}
X-Api-Key: pk_admin_4
Content-Type: application/json
{"query": "{ clubs(first: 10) { id name members(first: 10) { name } reviews(first: 5) { rating book { title } } } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.code" == "THROTTLED"
jsonpath "$.errors[0].extensions.cost.requestedQueryCost" == 430
jsonpath "$.errors[0].extensions.cost.throttleStatus.currentlyAvailable" < 430
jsonpath "$.data" not exists

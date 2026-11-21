# Lab 10.5: only Shelfie's own documents run. Hashes come from tests/run.sh (SHA-256 of each body).

# T01 Home by id alone
POST {{url}}
Content-Type: application/json
{"extensions": {"persistedQuery": {"version": 1, "sha256Hash": "{{home}}"}}}
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.clubs" count == 10

# T02 Club by id, with variables, as ada
POST {{url}}
Authorization: Bearer {{ada}}
Content-Type: application/json
{"variables": {"id": "c1"}, "extensions": {"persistedQuery": {"version": 1, "sha256Hash": "{{club}}"}}}
HTTP 200
[Asserts]
jsonpath "$.data.club.name" == "Night Owls"

# T03 A trusted mutation by id
POST {{url}}
Authorization: Bearer {{ada}}
Content-Type: application/json
{"variables": {"clubId": "c2", "bookId": "b1", "rating": 5, "body": "trusted"}, "extensions": {"persistedQuery": {"version": 1, "sha256Hash": "{{addreview}}"}}}
HTTP 200
[Asserts]
jsonpath "$.data.addReview.author.name" == "Ada"

# T04 An old client sending the exact text of a trusted document still works
POST {{url}}
Content-Type: application/json
{"query": "{{home_text}}"}
HTTP 200
[Asserts]
jsonpath "$.errors" not exists
jsonpath "$.data.clubs" count == 10

# T05 The experiment: a captured Home query, edited to ask for emails, replayed ad hoc
POST {{url}}
Content-Type: application/json
{"query": "query Home { clubs(first: 10) { id name members(first: 10) { name email } } }"}
HTTP *
[Asserts]
jsonpath "$.errors[0].message" contains "trusted"
jsonpath "$.data" not exists

# T06 A known id with different text: refused, not run
POST {{url}}
Content-Type: application/json
{"query": "query Home { readers(first: 40) { email } }", "extensions": {"persistedQuery": {"version": 1, "sha256Hash": "{{home}}"}}}
HTTP *
[Asserts]
jsonpath "$.errors" count >= 1
jsonpath "$.data" not exists

# T07 APQ is not trust: registering a new document with its own correct hash is refused
POST {{url}}
Content-Type: application/json
{"query": "{ readers(first: 40) { email } }", "extensions": {"persistedQuery": {"version": 1, "sha256Hash": "{{evil}}"}}}
HTTP *
[Asserts]
jsonpath "$.errors" count >= 1
jsonpath "$.data" not exists

# T08 ...and the id is still unknown afterwards
POST {{url}}
Content-Type: application/json
{"extensions": {"persistedQuery": {"version": 1, "sha256Hash": "{{evil}}"}}}
HTTP *
[Asserts]
jsonpath "$.errors" count >= 1
jsonpath "$.data" not exists

# T09 Introspection is not in the manifest
POST {{url}}
Content-Type: application/json
{"query": "{ __schema { types { name } } }"}
HTTP *
[Asserts]
jsonpath "$.data" not exists

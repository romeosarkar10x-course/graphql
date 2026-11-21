# Lab 9.6: automatic persisted queries over GET, and what an HTTP cache may keep.
# Hashes are SHA-256 of the exact query text (see the lab README for the four texts).

# P01 A hash the server has never seen: PersistedQueryNotFound, and never stored by a cache
GET {{url}}
[Query]
extensions: {"persistedQuery":{"version":1,"sha256Hash":"c2b9944cecec09516421c4e2ccaa971b4d6d8cdfac70dfeaf8ce6c19e53bf06f"}}
HTTP 200
[Asserts]
jsonpath "$.errors[0].message" == "PersistedQueryNotFound"
header "Cache-Control" contains "no-store"

# P02 The client retries with the text and the hash: registered, and executed
POST {{url}}
Content-Type: application/json
{"query": "{ club(id: 1) { name owner { name } } }", "extensions": {"persistedQuery": {"version": 1, "sha256Hash": "c2b9944cecec09516421c4e2ccaa971b4d6d8cdfac70dfeaf8ce6c19e53bf06f"}}}
HTTP 200
[Asserts]
jsonpath "$.data.club.name" == "Last Mirror Club"

# P03 From now on the hash alone, over GET, and the response may be shared
GET {{url}}
[Query]
extensions: {"persistedQuery":{"version":1,"sha256Hash":"c2b9944cecec09516421c4e2ccaa971b4d6d8cdfac70dfeaf8ce6c19e53bf06f"}}
HTTP 200
[Asserts]
jsonpath "$.data.club.owner.name" == "Asha Haddad"
header "Cache-Control" contains "public"

# P04 Registering text under a hash that isn't its hash: refused
POST {{url}}
Content-Type: application/json
{"query": "{ club(id: 4) { name } }", "extensions": {"persistedQuery": {"version": 1, "sha256Hash": "c9b6aa2e07262ba2c49b5020c907fef85dd6b18afc73be47f5bef1f630592632"}}}
HTTP 200
[Asserts]
jsonpath "$.errors[0].message" == "PersistedQueryIdInvalid"
jsonpath "$.data" not exists

# P05 A known hash with other text: refused too (graphql-java alone runs the cached document)
POST {{url}}
Content-Type: application/json
{"query": "{ club(id: 4) { name } }", "extensions": {"persistedQuery": {"version": 1, "sha256Hash": "c2b9944cecec09516421c4e2ccaa971b4d6d8cdfac70dfeaf8ce6c19e53bf06f"}}}
HTTP 200
[Asserts]
jsonpath "$.errors[0].message" == "PersistedQueryIdInvalid"

# P06 Plain GET with the query text works too
GET {{url}}
[Query]
query: { clubs(first: 2) { name } }
HTTP 200
[Asserts]
jsonpath "$.data.clubs" count == 2

# P07 GET never runs a mutation
GET {{url}}
[Query]
query: mutation { renameBook(id: 1, title: "via GET") { title } }
HTTP 405

# P08 Anything personal is private
GET {{url}}
X-Shelfie-User: 9
[Query]
extensions: {"persistedQuery":{"version":1,"sha256Hash":"c2b9944cecec09516421c4e2ccaa971b4d6d8cdfac70dfeaf8ce6c19e53bf06f"}}
HTTP 200
[Asserts]
header "Cache-Control" contains "private"

# P09 Through Varnish: the not-found answer is not kept, so registration takes effect at once
GET {{varnish}}
[Query]
extensions: {"persistedQuery":{"version":1,"sha256Hash":"c9b6aa2e07262ba2c49b5020c907fef85dd6b18afc73be47f5bef1f630592632"}}
HTTP 200
[Asserts]
jsonpath "$.errors[0].message" == "PersistedQueryNotFound"

POST {{url}}
Content-Type: application/json
{"query": "{ club(id: 3) { name memberCount } }", "extensions": {"persistedQuery": {"version": 1, "sha256Hash": "c9b6aa2e07262ba2c49b5020c907fef85dd6b18afc73be47f5bef1f630592632"}}}
HTTP 200

GET {{varnish}}
[Query]
extensions: {"persistedQuery":{"version":1,"sha256Hash":"c9b6aa2e07262ba2c49b5020c907fef85dd6b18afc73be47f5bef1f630592632"}}
HTTP 200
[Asserts]
jsonpath "$.data.club.name" == "Hollow River Club"
header "X-Cache" == "MISS"

# P10 ... and the next identical GET is served by Varnish
GET {{varnish}}
[Query]
extensions: {"persistedQuery":{"version":1,"sha256Hash":"c9b6aa2e07262ba2c49b5020c907fef85dd6b18afc73be47f5bef1f630592632"}}
HTTP 200
[Asserts]
header "X-Cache" == "HIT"
jsonpath "$.data.club.memberCount" == 25

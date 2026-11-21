# Lab 10.1: who is calling? Tokens come from tests/run.sh: real Keycloak tokens for ada and max, and
# bad ones minted with bin/course token.

# A01 No token: anonymous, viewer is null, and the request still works
POST {{url}}
Content-Type: application/json
{"query": "{ viewer { id } clubs(first: 1) { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.viewer" == null
jsonpath "$.data.clubs[0].id" == "c1"

# A02 A Keycloak token for ada: viewer is Ada
POST {{url}}
Authorization: Bearer {{kc_ada}}
Content-Type: application/json
{"query": "{ viewer { id name } }"}
HTTP 200
[Asserts]
jsonpath "$.data.viewer.id" == "r1"
jsonpath "$.data.viewer.name" == "Ada"

# A03 A Keycloak token for max: the roles claim reaches the app
POST {{url}}
Authorization: Bearer {{kc_max}}
Content-Type: application/json
{"query": "{ viewer { id roles } }"}
HTTP 200
[Asserts]
jsonpath "$.data.viewer.id" == "r3"
jsonpath "$.data.viewer.roles" includes "MODERATOR"

# A04 Expired token: 401 from the HTTP layer, with a Bearer challenge (RFC 6750)
POST {{url}}
Authorization: Bearer {{expired}}
Content-Type: application/json
{"query": "{ viewer { id } }"}
HTTP 401
[Asserts]
header "WWW-Authenticate" contains "invalid_token"

# A05 Forged token (right claims, wrong key): 401
POST {{url}}
Authorization: Bearer {{forged}}
Content-Type: application/json
{"query": "{ viewer { id } }"}
HTTP 401

# A06 Unsigned token ("alg": "none") claiming to be root: 401
POST {{url}}
Authorization: Bearer {{none}}
Content-Type: application/json
{"query": "{ viewer { id } }"}
HTTP 401

# A07 A token for another API (aud other-api): 401
POST {{url}}
Authorization: Bearer {{wrong_aud}}
Content-Type: application/json
{"query": "{ viewer { id } }"}
HTTP 401

# A08 A token from another issuer, signed with the right key: 401
POST {{url}}
Authorization: Bearer {{wrong_iss}}
Content-Type: application/json
{"query": "{ viewer { id } }"}
HTTP 401

# A09 Not a JWT at all: 401, not a GraphQL error
POST {{url}}
Authorization: Bearer not.a.token
Content-Type: application/json
{"query": "{ viewer { id } }"}
HTTP 401

# A10 A mutation with no token: a GraphQL error classified UNAUTHORIZED
POST {{url}}
Content-Type: application/json
{"query": "mutation { addReview(clubId: \"c2\", bookId: \"b1\", rating: 5, body: \"anon\") { id } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "UNAUTHORIZED"
jsonpath "$.data" == null

# A11 The same mutation as ada: the review is hers
POST {{url}}
Authorization: Bearer {{kc_ada}}
Content-Type: application/json
{"query": "mutation { addReview(clubId: \"c2\", bookId: \"b1\", rating: 5, body: \"signed in\") { author { name } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.addReview.author.name" == "Ada"

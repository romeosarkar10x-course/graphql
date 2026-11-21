# GraphQL over HTTP, September 2026 edition: a 25-request audit.
# https://http-spec.graphql.org/September2026/  (copy: _workbench/2026-11-21-m05-probe/)
# Run: bin/course hurl --test --variable url=http://localhost:4000/graphql audit.hurl
# Each entry names the rule it checks. Modelled on the graphql-http audit suite, written in Hurl
# because the course image has no Node.js.

# A01 "A server MUST support POST requests with a body encoded as UTF-8 JSON"
POST {{url}}
Accept: application/graphql-response+json
Content-Type: application/json
{"query": "{ hello }"}
HTTP 200
[Asserts]
header "Content-Type" startsWith "application/graphql-response+json"
jsonpath "$.data.hello" == "Hello from Shelfie"
jsonpath "$.errors" not exists

# A02 Content-Type with a charset parameter is still JSON
POST {{url}}
Accept: application/graphql-response+json
Content-Type: application/json; charset=utf-8
{"query": "{ hello }"}
HTTP 200
[Asserts]
jsonpath "$.data.hello" exists

# A03 "SHOULD indicate the encoding" in the response Content-Type
POST {{url}}
Accept: application/graphql-response+json
Content-Type: application/json
{"query": "{ hello }"}
HTTP 200
[Asserts]
header "Content-Type" contains "charset=utf-8"

# A04 Legacy client: Accept application/json gets application/json on 2xx
POST {{url}}
Accept: application/json
Content-Type: application/json
{"query": "{ hello }"}
HTTP 200
[Asserts]
header "Content-Type" startsWith "application/json"

# A05 "MUST respect the given Accept header": highest priority wins
POST {{url}}
Accept: application/json;q=0.5, application/graphql-response+json
Content-Type: application/json
{"query": "{ hello }"}
HTTP 200
[Asserts]
header "Content-Type" startsWith "application/graphql-response+json"

# A06 No Accept header at all still works
POST {{url}}
Content-Type: application/json
{"query": "{ hello }"}
HTTP 200
[Asserts]
jsonpath "$.data.hello" exists

# A07 Nothing acceptable, and no application/json: 406 SHOULD be used
POST {{url}}
Accept: text/html
Content-Type: application/json
{"query": "{ hello }"}
HTTP 406

# A08 Unsupported request Content-Type: 415 SHOULD be used
POST {{url}}
Accept: application/graphql-response+json
Content-Type: text/plain
```
{ hello }
```
HTTP 415

# A09 "If the JSON body of the request could not be parsed, status code 400 SHOULD be used"
POST {{url}}
Accept: application/graphql-response+json
Content-Type: application/json
`{"query":`
HTTP 400
[Asserts]
header "Content-Type" not startsWith "application/graphql-response+json"

# A10 Not a well-formed request (typo in "query"): 422
POST {{url}}
Accept: application/graphql-response+json
Content-Type: application/json
{"qeury": "{ hello }"}
HTTP 422

# A11 query of the wrong type: 422
POST {{url}}
Accept: application/graphql-response+json
Content-Type: application/json
{"query": 7}
HTTP 422

# A12 variables of the wrong shape (an array): 422
POST {{url}}
Accept: application/graphql-response+json
Content-Type: application/json
{"query": "query Q($id: ID!) { club(id: $id) { name } }", "variables": ["c1"]}
HTTP 422

# A13 The body is JSON but not an object: 422
POST {{url}}
Accept: application/graphql-response+json
Content-Type: application/json
["{ hello }"]
HTTP 422

# A14 "If the GraphQL document in the request cannot be parsed, status code 400 SHOULD be used"
POST {{url}}
Accept: application/graphql-response+json
Content-Type: application/json
{"query": "{"}
HTTP 400
[Asserts]
header "Content-Type" startsWith "application/graphql-response+json"
jsonpath "$.errors" count >= 1
jsonpath "$.data" not exists

# A15 "If the request does not pass GraphQL validation, status code 422 SHOULD be used"
POST {{url}}
Accept: application/graphql-response+json
Content-Type: application/json
{"query": "{ club(id: \"c1\") { founder } }"}
HTTP 422
[Asserts]
jsonpath "$.errors[0].message" contains "founder"
jsonpath "$.data" not exists

# A16 Validation failure for a legacy client: still 422 (only 2xx swap the media type)
POST {{url}}
Accept: application/json
Content-Type: application/json
{"query": "{ club(id: \"c1\") { founder } }"}
HTTP 422

# A17 Two operations and no operationName: the operation cannot be determined, 422
POST {{url}}
Accept: application/graphql-response+json
Content-Type: application/json
{"query": "query A { hello } query B { clubs { id } }"}
HTTP 422

# A18 operationName selects one of two operations
POST {{url}}
Accept: application/graphql-response+json
Content-Type: application/json
{"query": "query A { hello } query B { clubs { id } }", "operationName": "B"}
HTTP 200
[Asserts]
jsonpath "$.data.clubs" count == 3
jsonpath "$.data.hello" not exists

# A19 "If the variable values cannot be coerced", 422
POST {{url}}
Accept: application/graphql-response+json
Content-Type: application/json
{"query": "query Q($first: Int) { books(first: $first) { id } }", "variables": {"first": "three"}}
HTTP 422

# A20 Data and errors (a field error): partial success, 294 SHOULD be used
POST {{url}}
Accept: application/graphql-response+json
Content-Type: application/json
{"query": "{ hello boom }"}
HTTP 294
[Asserts]
header "Content-Type" startsWith "application/graphql-response+json"
jsonpath "$.data.hello" == "Hello from Shelfie"
jsonpath "$.errors[0].path[0]" == "boom"

# A21 The same partial success for a legacy client: a 2xx it understands
POST {{url}}
Accept: application/json
Content-Type: application/json
{"query": "{ hello boom }"}
HTTP 200
[Asserts]
header "Content-Type" startsWith "application/json"
jsonpath "$.errors" count == 1

# A22 "Servers receiving a request with additional properties MUST ignore" them; extensions is a map
POST {{url}}
Accept: application/graphql-response+json
Content-Type: application/json
{"query": "{ hello }", "extensions": {"trace": true}, "somethingNew": 1}
HTTP 200

# A23 GET with the parameters in the URL, variables as a JSON string
GET {{url}}
Accept: application/graphql-response+json
[Query]
query: query Q($id: ID!) { club(id: $id) { name } }
variables: {"id": "c2"}
operationName:
HTTP 200
[Asserts]
jsonpath "$.data.club.name" == "Slow Burn Mysteries"

# A24 "GET requests MUST NOT be used for executing mutation operations": 405 with Allow
GET {{url}}
Accept: application/graphql-response+json
[Query]
query: mutation { createReview(input: {bookId: "b1", rating: 5, body: "via GET"}) { review { id } } }
HTTP 405
[Asserts]
header "Allow" contains "POST"

# A25 An unsupported method: 405, and RFC 9110 requires the Allow header
PUT {{url}}
Content-Type: application/json
{"query": "{ hello }"}
HTTP 405
[Asserts]
header "Allow" contains "POST"

# Lab 8.6 checks: the error contract, over HTTP.

# X01 An outage is masked: a generic message, a code, an errorId equal to the response's requestId; data survives
POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b2\") { title coverUrl } }"}
HTTP 200
[Captures]
request_id: jsonpath "$.extensions.requestId"
[Asserts]
jsonpath "$.data.book.title" == "Piranesi"
jsonpath "$.data.book.coverUrl" == null
jsonpath "$.errors[0].path[1]" == "coverUrl"
jsonpath "$.errors[0].message" == "Internal error"
jsonpath "$.errors[0].extensions.code" == "INTERNAL"
jsonpath "$.errors[0].extensions.errorId" == {{request_id}}
jsonpath "$.errors[0].message" not contains "s3"
jsonpath "$.errors[0].message" not contains "10.0"

# X02 A validation error has a code too, and no data
POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b2\") { founder } }"}
HTTP *
[Asserts]
jsonpath "$.errors[0].extensions.code" == "INVALID_REQUEST"
jsonpath "$.data" not exists
jsonpath "$.extensions.requestId" exists

# X03 So does a syntax error
POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b2\") { title "}
HTTP *
[Asserts]
jsonpath "$.errors[0].extensions.code" == "INVALID_REQUEST"

# X04 A missing variable
POST {{url}}
Content-Type: application/json
{"query": "query B($id: ID!) { book(id: $id) { title } }"}
HTTP *
[Asserts]
jsonpath "$.errors[0].extensions.code" == "INVALID_REQUEST"

# X05 No viewer: UNAUTHENTICATED, with the domain message kept
POST {{url}}
Content-Type: application/json
{"query": "{ myClubs { id } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.code" == "UNAUTHENTICATED"
jsonpath "$.errors[0].extensions.classification" == "UNAUTHENTICATED"
jsonpath "$.errors[0].message" contains "Sign in"

# X06 With a viewer: no error
POST {{url}}
Content-Type: application/json
X-Shelfie-User: u1
{"query": "{ myClubs { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.myClubs" count == 2
jsonpath "$.errors" not exists

# X07 A private club you aren't in, and a club that doesn't exist: the same NOT_FOUND
POST {{url}}
Content-Type: application/json
X-Shelfie-User: u2
{"query": "mutation { joinClub(clubId: \"c3\") { id } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.code" == "NOT_FOUND"

POST {{url}}
Content-Type: application/json
X-Shelfie-User: u2
{"query": "mutation { joinClub(clubId: \"c99\") { id } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.code" == "NOT_FOUND"

# X08 Join once, then CONFLICT
POST {{url}}
Content-Type: application/json
X-Shelfie-User: u12
{"query": "mutation { joinClub(clubId: \"c1\") { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.joinClub.id" == "c1"

POST {{url}}
Content-Type: application/json
X-Shelfie-User: u12
{"query": "mutation { joinClub(clubId: \"c1\") { id } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.code" == "CONFLICT"

# X09 Someone else's review: FORBIDDEN; your own: deleted; then NOT_FOUND
POST {{url}}
Content-Type: application/json
X-Shelfie-User: u2
{"query": "mutation { deleteReview(id: \"r1\") }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.code" == "FORBIDDEN"

POST {{url}}
Content-Type: application/json
X-Shelfie-User: u1
{"query": "mutation { deleteReview(id: \"r1\") }"}
HTTP 200
[Asserts]
jsonpath "$.data.deleteReview" == true

POST {{url}}
Content-Type: application/json
X-Shelfie-User: u1
{"query": "mutation { deleteReview(id: \"r1\") }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.code" == "NOT_FOUND"

# X10 The client's X-Request-Id is used when it is sane, replaced when it isn't
POST {{url}}
Content-Type: application/json
X-Request-Id: hurl-check-42
{"query": "{ hello }"}
HTTP 200
[Asserts]
jsonpath "$.extensions.requestId" == "hurl-check-42"

POST {{url}}
Content-Type: application/json
X-Request-Id: <script>
{"query": "{ hello }"}
HTTP 200
[Asserts]
jsonpath "$.extensions.requestId" exists
jsonpath "$.extensions.requestId" != "<script>"

# Lab 14.6, part 1: masking, soft delete, audit. Tokens: olu (oluwaseun, u5), ada (u1), root (admin).

# S01 your own personal data, whole
POST {{url}}
Authorization: Bearer {{olu}}
Content-Type: application/json
{"query": "{ me { id email phone } }"}
HTTP 200
[Asserts]
jsonpath "$.data.me.id" == "u5"
jsonpath "$.data.me.email" == "oluwaseun.adeyemi@example.net"
jsonpath "$.data.me.phone" == "+234 803 555 0147"

# S02 someone else's, masked (this also puts u5 in any profile cache)
POST {{url}}
Authorization: Bearer {{ada}}
Content-Type: application/json
{"query": "{ user(id: \"u5\") { name email phone reviews { id } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.user.name" == "Oluwaseun Adeyemi"
jsonpath "$.data.user.email" == "*************************.net"
jsonpath "$.data.user.phone" == "***************47"
jsonpath "$.data.user.reviews" count == 2

# S03 a review whose text is personal (the audit must not keep it)
POST {{url}}
Authorization: Bearer {{olu}}
Content-Type: application/json
{"query": "mutation PostReview { createReview(bookId: \"b1\", rating: 4, body: \"MARKER-9431: finished it at my sister's flat\") { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.createReview.id" exists

# S04 soft delete: gone from every read at once
POST {{url}}
Authorization: Bearer {{ada}}
Content-Type: application/json
{"query": "mutation { deleteReview(id: \"r1\") }"}
HTTP 200
[Asserts]
jsonpath "$.data.deleteReview" == "r1"

POST {{url}}
Authorization: Bearer {{ada}}
Content-Type: application/json
{"query": "{ review(id: \"r1\") { id } book(id: \"b1\") { reviews { id } } me { reviews { id } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.review" == null
jsonpath "$.data.book.reviews[*].id" not contains "r1"
jsonpath "$.data.me.reviews" count == 0

# S05 and back: the author can restore it
POST {{url}}
Authorization: Bearer {{ada}}
Content-Type: application/json
{"query": "mutation { restoreReview(id: \"r1\") { id body } }"}
HTTP 200
[Asserts]
jsonpath "$.data.restoreReview.id" == "r1"
jsonpath "$.data.restoreReview.body" == "The ice crossing."

# S06 only the author may delete
POST {{url}}
Authorization: Bearer {{ada}}
Content-Type: application/json
{"query": "mutation Vandal { deleteReview(id: \"r3\") }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "FORBIDDEN"

# S07 the audit log is for admins
POST {{url}}
Authorization: Bearer {{ada}}
Content-Type: application/json
{"query": "{ auditLog { field } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "FORBIDDEN"

# S08 all four mutation fields so far, newest first, who and what and how it ended, never the values
POST {{url}}
Authorization: Bearer {{root}}
Content-Type: application/json
{"query": "{ auditLog(last: 5) { at actor operation field arguments outcome } }"}
HTTP 200
[Asserts]
body not contains "MARKER-9431"
body not contains "sister"
jsonpath "$.data.auditLog" count == 4
jsonpath "$.data.auditLog[0].operation" == "Vandal"
jsonpath "$.data.auditLog[0].field" == "deleteReview"
jsonpath "$.data.auditLog[0].actor" == "u1"
jsonpath "$.data.auditLog[0].outcome" == "error"
jsonpath "$.data.auditLog[0].arguments[0]" == "id"
jsonpath "$.data.auditLog[1].field" == "restoreReview"
jsonpath "$.data.auditLog[1].outcome" == "ok"
jsonpath "$.data.auditLog[2].field" == "deleteReview"
jsonpath "$.data.auditLog[2].actor" == "u1"
jsonpath "$.data.auditLog[3].field" == "createReview"
jsonpath "$.data.auditLog[3].actor" == "u5"
jsonpath "$.data.auditLog[3].operation" == "PostReview"
jsonpath "$.data.auditLog[3].arguments" count == 3
jsonpath "$.data.auditLog[3].arguments[2]" == "body"
jsonpath "$.data.auditLog[3].at" matches /^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d/

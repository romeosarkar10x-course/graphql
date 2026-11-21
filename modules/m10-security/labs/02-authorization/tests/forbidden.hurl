# Lab 10.2: every forbidden path, for anonymous, member, moderator, owner and admin.
# ada: member of c1, owns c2. ben: member of c2, owns c3. max: moderator, member of c1. olu: owns c1. root: admin.
# c1 and c3 are private. Reviews v1-v30 are in c1; v31-v33 ada's, v34-v36 ben's, v37-v39 r13's, all in c2.

# Z01 Anonymous: clubs lists only the 10 public clubs
POST {{url}}
Content-Type: application/json
{"query": "{ clubs(first: 12) { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.clubs" count == 10
jsonpath "$.data.clubs[*].id" not includes "c1"

# Z02 ada sees her private club c1 but not c3
POST {{url}}
Authorization: Bearer {{ada}}
Content-Type: application/json
{"query": "{ clubs(first: 12) { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.clubs" count == 11
jsonpath "$.data.clubs[*].id" includes "c1"
jsonpath "$.data.clubs[*].id" not includes "c3"

# Z03 ben asks for c1 directly: null
POST {{url}}
Authorization: Bearer {{ben}}
Content-Type: application/json
{"query": "{ club(id: \"c1\") { name } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club" == null

# Z04 ben asks for c1 through node(id:): null too
POST {{url}}
Authorization: Bearer {{ben}}
Content-Type: application/json
{"query": "{ node(id: \"c1\") { id ... on Club { name } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.node" == null

# Z05 ben asks for a c1 review through node(id:): null
POST {{url}}
Authorization: Bearer {{ben}}
Content-Type: application/json
{"query": "{ node(id: \"v1\") { ... on Review { body } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.node" == null

# Z06 Anonymous asks for a c3 review through node(id:): null
POST {{url}}
Content-Type: application/json
{"query": "{ node(id: \"v61\") { ... on Review { body } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.node" == null

# Z07 ben walks every book's reviews: none from c1 (the connection leak)
POST {{url}}
Authorization: Bearer {{ben}}
Content-Type: application/json
{"query": "{ books(first: 60) { reviews(first: 50) { club { id } } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books[*].reviews[*].club.id" not includes "c1"
jsonpath "$.data.books[*].reviews[*].club.id" includes "c3"

# Z08 Anonymous walks every book's reviews: no private club at all
POST {{url}}
Content-Type: application/json
{"query": "{ books(first: 60) { reviews(first: 50) { club { private } } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.books[*].reviews[*].club.private" not includes true

# Z09 ben looks at olu's clubs: c1 is hidden
POST {{url}}
Authorization: Bearer {{ben}}
Content-Type: application/json
{"query": "{ node(id: \"r4\") { ... on Reader { clubs(first: 12) { id } } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.node.clubs[*].id" not includes "c1"

# Z10 ben reads ada's email: null
POST {{url}}
Authorization: Bearer {{ben}}
Content-Type: application/json
{"query": "{ node(id: \"r1\") { ... on Reader { name email } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.node.name" == "Ada"
jsonpath "$.data.node.email" == null

# Z11 ada reads her own email
POST {{url}}
Authorization: Bearer {{ada}}
Content-Type: application/json
{"query": "{ viewer { email } }"}
HTTP 200
[Asserts]
jsonpath "$.data.viewer.email" == "ada@shelfie.test"

# Z12 root (admin) reads ada's email
POST {{url}}
Authorization: Bearer {{root}}
Content-Type: application/json
{"query": "{ node(id: \"r1\") { ... on Reader { email } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.node.email" == "ada@shelfie.test"

# Z13 ada (a member of c1, not its owner) reads c1's invite code: null
POST {{url}}
Authorization: Bearer {{ada}}
Content-Type: application/json
{"query": "{ club(id: \"c1\") { name inviteCode } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.name" == "Night Owls"
jsonpath "$.data.club.inviteCode" == null

# Z14 olu (owner of c1) reads it
POST {{url}}
Authorization: Bearer {{olu}}
Content-Type: application/json
{"query": "{ club(id: \"c1\") { inviteCode } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.inviteCode" == "4821"

# Z15 Anonymous lists readers: UNAUTHORIZED
POST {{url}}
Content-Type: application/json
{"query": "{ readers { id } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "UNAUTHORIZED"

# Z16 ada (member) lists readers: FORBIDDEN, by the @auth directive
POST {{url}}
Authorization: Bearer {{ada}}
Content-Type: application/json
{"query": "{ readers { id } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "FORBIDDEN"
jsonpath "$.errors[0].path[0]" == "readers"

# Z17 max (moderator) lists readers
POST {{url}}
Authorization: Bearer {{max}}
Content-Type: application/json
{"query": "{ readers(first: 3) { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.readers" count == 3
jsonpath "$.errors" not exists

# Z18 root (admin) lists readers: admin passes every role check
POST {{url}}
Authorization: Bearer {{root}}
Content-Type: application/json
{"query": "{ readers(first: 3) { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.readers" count == 3

# Z19 ben adds a review to c1, a club he is not in: FORBIDDEN
POST {{url}}
Authorization: Bearer {{ben}}
Content-Type: application/json
{"query": "mutation { addReview(clubId: \"c1\", bookId: \"b1\", rating: 1, body: \"gatecrash\") { id } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "FORBIDDEN"

# Z20 ada adds a review to c1, her own club
POST {{url}}
Authorization: Bearer {{ada}}
Content-Type: application/json
{"query": "mutation { addReview(clubId: \"c1\", bookId: \"b1\", rating: 4, body: \"member note\") { club { id } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.addReview.club.id" == "c1"

# Z21 ben removes r13's review in c2 (not his, not his club, not a moderator): FORBIDDEN
POST {{url}}
Authorization: Bearer {{ben}}
Content-Type: application/json
{"query": "mutation { removeReview(id: \"v37\") }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "FORBIDDEN"

# Z22 ada, owner of c2, removes it
POST {{url}}
Authorization: Bearer {{ada}}
Content-Type: application/json
{"query": "mutation { removeReview(id: \"v37\") }"}
HTTP 200
[Asserts]
jsonpath "$.data.removeReview" == "v37"

# Z23 max, a moderator, removes another of r13's
POST {{url}}
Authorization: Bearer {{max}}
Content-Type: application/json
{"query": "mutation { removeReview(id: \"v38\") }"}
HTTP 200
[Asserts]
jsonpath "$.data.removeReview" == "v38"

# Z24 ben removes his own review
POST {{url}}
Authorization: Bearer {{ben}}
Content-Type: application/json
{"query": "mutation { removeReview(id: \"v34\") }"}
HTTP 200
[Asserts]
jsonpath "$.data.removeReview" == "v34"

# Z25 ada removes olu's review in c1 (member, not owner, not author): FORBIDDEN
POST {{url}}
Authorization: Bearer {{ada}}
Content-Type: application/json
{"query": "mutation { removeReview(id: \"v1\") }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "FORBIDDEN"

# Z26 ada renames c1, which olu owns: FORBIDDEN
POST {{url}}
Authorization: Bearer {{ada}}
Content-Type: application/json
{"query": "mutation { renameClub(id: \"c1\", name: \"Ada's Owls\") { name } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "FORBIDDEN"

# Z27 olu renames c1
POST {{url}}
Authorization: Bearer {{olu}}
Content-Type: application/json
{"query": "mutation { renameClub(id: \"c1\", name: \"Night Owls\") { name } }"}
HTTP 200
[Asserts]
jsonpath "$.data.renameClub.name" == "Night Owls"

# Z28 Anonymous renames a club: UNAUTHORIZED
POST {{url}}
Content-Type: application/json
{"query": "mutation { renameClub(id: \"c2\", name: \"x\") { name } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "UNAUTHORIZED"

# Z29 ben joins c1 with the right code; now he can see it
POST {{url}}
Authorization: Bearer {{ben}}
Content-Type: application/json
{"query": "mutation { joinClub(id: \"c1\", inviteCode: \"4821\") { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.joinClub.id" == "c1"

POST {{url}}
Authorization: Bearer {{ben}}
Content-Type: application/json
{"query": "{ club(id: \"c1\") { reviews(first: 1) { id } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.reviews[0].id" == "v1"

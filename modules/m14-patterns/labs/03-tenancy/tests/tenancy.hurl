# Lab 14.3: ada (tenant acme, basic plan) tries to read globex's data six ways; max is globex (premium).
# Variables: acme, globex (tokens), notenant (a valid token with no tenant claim), forged.

# P01 each tenant sees its own clubs
POST {{url}}
Authorization: Bearer {{acme}}
Content-Type: application/json
{"query": "{ viewer { handle tenant plan } clubs { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.viewer.tenant" == "acme"
jsonpath "$.data.viewer.plan" == "basic"
jsonpath "$.data.clubs[*].id" count == 2
jsonpath "$.data.clubs[0].id" == "c-a1"
jsonpath "$.data.clubs[1].id" == "c-a2"

# P02 globex warms whatever caches there are with its own club first
POST {{url}}
Authorization: Bearer {{globex}}
Content-Type: application/json
{"query": "{ clubs { id } club(id: \"c-g1\") { name members } node(id: \"c-g1\") { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.clubs[*].id" count == 2
jsonpath "$.data.club.name" == "Globex Readers"
jsonpath "$.data.node.id" == "c-g1"

# A01 path 1, by id
POST {{url}}
Authorization: Bearer {{acme}}
Content-Type: application/json
{"query": "{ club(id: \"c-g1\") { name members } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club" == null

# A02 path 2, by node id (books still resolve: the catalogue is shared)
POST {{url}}
Authorization: Bearer {{acme}}
Content-Type: application/json
{"query": "{ club: node(id: \"c-g2\") { id ... on Club { name } } book: node(id: \"b2\") { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club" == null
jsonpath "$.data.book.id" == "b2"

# A03 path 3, search
POST {{url}}
Authorization: Bearer {{acme}}
Content-Type: application/json
{"query": "{ readers: search(text: \"readers\") { id } globex: search(text: \"globex\") { id } }"}
HTTP 200
[Asserts]
jsonpath "$.data.readers" count == 1
jsonpath "$.data.readers[0].id" == "c-a1"
jsonpath "$.data.globex" count == 0

# A04 path 4, through the shared catalogue: a book's clubs
POST {{url}}
Authorization: Bearer {{acme}}
Content-Type: application/json
{"query": "{ book(id: \"b1\") { title clubsReading { id name } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.book.title" == "The Left Hand of Darkness"
jsonpath "$.data.book.clubsReading" count == 1
jsonpath "$.data.book.clubsReading[0].id" == "c-a1"

# A05 path 5, a mutation: joining another tenant's club looks exactly like joining a club that does not exist
POST {{url}}
Authorization: Bearer {{acme}}
Content-Type: application/json
{"query": "mutation { theirs: joinClub(clubId: \"c-g1\") { club { name members } userErrors { message } } none: joinClub(clubId: \"c-zz\") { club { name } userErrors { message } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.theirs.club" == null
jsonpath "$.data.theirs.userErrors[0].message" exists
jsonpath "$.data.theirs.userErrors[0].message" not contains "Globex"
[Captures]
theirs: jsonpath "$.data.theirs.userErrors[0].message" replace "c-g1" "ID"
none: jsonpath "$.data.none.userErrors[0].message" replace "c-zz" "ID"

# A05b the two messages differ only by the id
POST {{url}}
Authorization: Bearer {{acme}}
Content-Type: application/json
{"query": "{ __typename }"}
HTTP 200
[Asserts]
variable "theirs" == "{{none}}"

# A06 path 6, the header: a client-chosen X-Tenant changes nothing
POST {{url}}
Authorization: Bearer {{acme}}
X-Tenant: globex
Content-Type: application/json
{"query": "{ viewer { tenant } clubs { id } club(id: \"c-g1\") { name } }"}
HTTP 200
[Asserts]
jsonpath "$.data.viewer.tenant" == "acme"
jsonpath "$.data.clubs[0].id" == "c-a1"
jsonpath "$.data.club" == null

# U01 no token: no tenant, no data
POST {{url}}
Content-Type: application/json
{"query": "{ clubs { id } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "UNAUTHORIZED"
jsonpath "$.data" == null

# U02 a valid token that names no tenant is not a tenant either
POST {{url}}
Authorization: Bearer {{notenant}}
Content-Type: application/json
{"query": "{ clubs { id } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "UNAUTHORIZED"

# U03 a forged token never reaches GraphQL
POST {{url}}
Authorization: Bearer {{forged}}
Content-Type: application/json
{"query": "{ clubs { id } }"}
HTTP 401

# V01 premium: analytics exists
POST {{url}}
Authorization: Bearer {{globex}}
Content-Type: application/json
{"query": "{ __type(name: \"Club\") { fields { name } } clubs { analytics { memberCount } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.__type.fields[*].name" contains "analytics"
jsonpath "$.data.clubs[0].analytics.memberCount" == 2

# V02 basic: analytics does not exist, in introspection or in validation
POST {{url}}
Authorization: Bearer {{acme}}
Content-Type: application/json
{"query": "{ __type(name: \"Club\") { fields { name } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.__type.fields[*].name" not contains "analytics"
jsonpath "$.data.__type.fields[*].name" contains "members"

# V03 basic: selecting it is a validation error, like any unknown field
POST {{url}}
Authorization: Bearer {{acme}}
Content-Type: application/json
{"query": "{ clubs { analytics { memberCount } } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "ValidationError"
jsonpath "$.data" not exists

# J01 joining your own tenant's club works
POST {{url}}
Authorization: Bearer {{acme}}
Content-Type: application/json
{"query": "mutation { joinClub(clubId: \"c-a1\") { club { id members } userErrors { message } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.joinClub.userErrors" count == 0
jsonpath "$.data.joinClub.club.members" contains "ada"

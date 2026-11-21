# Lab 6.4 checks.

# T01 The tenant reaches a field three levels down
POST {{url}}
Content-Type: application/json
X-Shelfie-Tenant: eu
{"query": "{ club(id: \"c1\") { meetups { startsAt startsAtLocal } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.meetups[0].startsAtLocal" == "2026-11-27T20:00+01:00[Europe/Paris]"

# T02 Another tenant, another zone, same request otherwise
POST {{url}}
Content-Type: application/json
X-Shelfie-Tenant: in
{"query": "{ club(id: \"c1\") { meetups { startsAtLocal } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.meetups[0].startsAtLocal" == "2026-11-28T00:30+05:30[Asia/Kolkata]"

# T03 No header (or an unknown one): UTC, tenant "global"
POST {{url}}
Content-Type: application/json
X-Shelfie-Tenant: mars
{"query": "{ club(id: \"c2\") { meetups { startsAtLocal } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.meetups[0].startsAtLocal" == "2026-11-25T18:30Z[UTC]"
jsonpath "$.extensions.tenant" == "global"

# T04 The response says which tenant it served: header and extensions
POST {{url}}
Content-Type: application/json
X-Shelfie-Tenant: us
{"query": "{ hello }"}
HTTP 200
[Asserts]
header "X-Shelfie-Tenant-Served" == "us"
jsonpath "$.extensions.tenant" == "us"

# T05 @uppercase is applied by wiring, not by the controller
POST {{url}}
Content-Type: application/json
{"query": "{ club(id: \"c3\") { meetups { title headline } } }"}
HTTP 200
[Asserts]
jsonpath "$.data.club.meetups[0].title" == "Shortlist debrief"
jsonpath "$.data.club.meetups[0].headline" == "SHORTLIST DEBRIEF"

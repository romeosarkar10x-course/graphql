# Scenario 5: "Through the CDN (Varnish), reader 8 saw reader 7's profile."
# GET requests, through {{varnish}} (http://varnish:6081/graphql inside the course image).
GET {{varnish}}
X-Shelfie-User: 7
[Query]
query: { me: viewer { handle name } }
HTTP 200
[Asserts]
jsonpath "$.data.me.handle" == "reader7"

GET {{varnish}}
X-Shelfie-User: 8
[Query]
query: { me: viewer { handle name } }
HTTP 200
[Asserts]
jsonpath "$.data.me.handle" == "reader8"

# The anonymous club list may be shared; nothing personal may be stored.
GET {{url}}
[Query]
query: { clubs(first: 3) { name } }
HTTP 200
[Asserts]
header "Cache-Control" contains "public"

GET {{url}}
X-Shelfie-User: 7
[Query]
query: { me: viewer { handle } }
HTTP 200
[Asserts]
header "Cache-Control" contains "private"

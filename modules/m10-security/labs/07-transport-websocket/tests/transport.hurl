# Lab 10.7: CORS and error messages.

# H01 A preflight from a hostile origin gets no CORS permission
OPTIONS {{url}}
Origin: http://evil.test
Access-Control-Request-Method: POST
Access-Control-Request-Headers: content-type, authorization
HTTP *
[Asserts]
header "Access-Control-Allow-Origin" not exists

# H02 Shelfie's own web app may call, without cookies
OPTIONS {{url}}
Origin: https://shelfie.test
Access-Control-Request-Method: POST
Access-Control-Request-Headers: content-type, authorization
HTTP 200
[Asserts]
header "Access-Control-Allow-Origin" == "https://shelfie.test"
header "Access-Control-Allow-Credentials" not exists

# H03 A failing resolver: the client learns that it failed, not the internals
POST {{url}}
Content-Type: application/json
{"query": "{ book(id: \"b1\") { recommendations(first: 11) { id } } }"}
HTTP 200
[Asserts]
jsonpath "$.errors[0].extensions.classification" == "INTERNAL_ERROR"
jsonpath "$.errors[0].message" not contains "recs.internal"
jsonpath "$.errors[0].message" not contains "sk_live"

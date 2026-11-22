# Game 14.9 at 23:30 UTC on Thursday 9 July 2026: already Friday in London (BST, UTC+1).

# F01 Friday in the server's zone: redacted, with each field's own replacement
POST {{url}}
Content-Type: application/json
{"query": "{ quotes { ending blame } }"}
HTTP 200
[Asserts]
jsonpath "$.data.quotes[0].ending" == "[redacted]"
jsonpath "$.data.quotes[0].blame" == "nobody you know"
jsonpath "$.data.quotes[3].ending" == "[redacted]"

# F02 null stays null, even on Fridays
POST {{url}}
Content-Type: application/json
{"query": "{ quotes { ending blame } }"}
HTTP 200
[Asserts]
jsonpath "$.data.quotes[2].ending" == null
jsonpath "$.data.quotes[1].blame" == null

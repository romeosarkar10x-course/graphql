# Scenario 3: "I signed in as reader 6 and the app said hello to reader 5."
POST {{url}}
X-Shelfie-User: 5
Content-Type: application/json
{"query": "{ viewer { handle } }"}
HTTP 200
[Asserts]
jsonpath "$.data.viewer.handle" == "reader5"

POST {{url}}
X-Shelfie-User: 6
Content-Type: application/json
{"query": "{ viewer { handle } }"}
HTTP 200
[Asserts]
jsonpath "$.data.viewer.handle" == "reader6"

# Game 14.9 on a Thursday (the server's clock is pinned by the grader).

# Z01 @pirate: the table, capitals kept, Arr! at the end
POST {{url}}
Content-Type: application/json
{"query": "{ quotes { id pirate } }"}
HTTP 200
[Asserts]
jsonpath "$.data.quotes[0].pirate" == "Ahoy me matey, th' sea be calm tonight. Arr!"
jsonpath "$.data.quotes[2].pirate" == "Be ye a Book person? Aye, I love books and th' night stars. Arr!"
jsonpath "$.data.quotes[4].pirate" == "Winter be long; th' sun be low; me mateys be near. Arr!"

# Z02 @pirate on whole words only: "Hello" inside "Othello" or "is" inside "this" stays
POST {{url}}
Content-Type: application/json
{"query": "{ quotes { pirate } }"}
HTTP 200
[Asserts]
jsonpath "$.data.quotes[3].pirate" == "Th' Beauty of th' House be immeasurable; its Kindness infinite. Arr!"
jsonpath "$.data.quotes[1].pirate" == "Light be th' left hand of darkness, and darkness th' right hand of light. Arr!"

# Z03 @emojify: whole words, ignoring case, nothing else
POST {{url}}
Content-Type: application/json
{"query": "{ quotes { chat } }"}
HTTP 200
[Asserts]
jsonpath "$.data.quotes[0].chat" == "Hello my friend, the 🌊 is calm tonight."
jsonpath "$.data.quotes[2].chat" == "Are you a 📚 person? Yes, I ❤️ 📚📚 and the 🌙 ⭐⭐."
jsonpath "$.data.quotes[4].chat" == "❄️ is long; the ☀️ is low; my friends are near."

# Z04 not Friday: nothing redacted, nulls stay null
POST {{url}}
Content-Type: application/json
{"query": "{ quotes { ending blame } }"}
HTTP 200
[Asserts]
jsonpath "$.data.quotes[0].ending" == "Genly goes home alone."
jsonpath "$.data.quotes[0].blame" == "Estraven"
jsonpath "$.data.quotes[2].ending" == null
jsonpath "$.data.quotes[1].blame" == null

# Z05 stacked: emoji first, then the pirate (the order they are written)
POST {{url}}
Content-Type: application/json
{"query": "{ quotes { shanty } }"}
HTTP 200
[Asserts]
jsonpath "$.data.quotes[0].shanty" == "Ahoy me matey, th' 🌊 be calm tonight. Arr!"
jsonpath "$.data.quotes[4].shanty" == "❄️ be long; th' ☀️ be low; me mateys be near. Arr!"

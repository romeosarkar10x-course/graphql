#!/usr/bin/env bash
# The refusals of lab 18.3's run_graphql, word for word.
source /course/tools/testlib.sh; source /course/modules/m06-spring/contract/app-test.sh; source /course/modules/m18-frontier/contract/mcp.sh
cd /course/modules/m18-frontier/labs/03-agent-safety/solution
app_start java -jar target/shelfie-mcp.jar || exit 1
mcp_init http://localhost:$PORT/mcp "X-Shelfie-User: u4"
for q in 'mutation { deleteReview(input: {id: "r1"}) { deletedReviewId } }' '{ __schema { types { name } } }' \
  '{ clubs { members(first: 50) { clubs { members(first: 50) { name } } } } }' 'query($n: Int = 500) { books(first: $n) { title } }' \
  '{ a: books(first: 50) { title } b: books(first: 50) { title } c: books(first: 50) { title } }' \
  '{ club(id: "c1") { members { clubs { members { clubs { members { clubs { name } } } } } } } }' '{ books(first: 100) { title } }' '{ clubs { name } }'; do
  echo "$q"; mcp_call run_graphql "$(jq -cn --arg q "$q" '{query: $q}')" | jq -r '"  -> isError=\(.isError): \(.content[0].text | .[0:220])"'
done

#!/usr/bin/env bash
source /course/tools/testlib.sh
source /course/modules/m06-spring/contract/app-test.sh
source /course/modules/m18-frontier/contract/mcp.sh
cd "$LAB_DIR" || exit 1
OPS=/course/modules/m18-frontier/contract/agent/operations
app_build target/shelfie-mcp.jar || finish
app_start java -jar target/shelfie-mcp.jar || finish
mcp_init http://localhost:$PORT/mcp && pass "an MCP session opens at /mcp (Streamable HTTP)" || { fail "an MCP session opens at /mcp" "$(tail -3 /tmp/app.log)"; finish; }

tools=$(mcp_tools)
want="book_details club_details club_meetups find_books list_clubs reader_profile reader_reviews search"
have=$(jq -r '[.[].name] | sort | join(" ")' <<<"$tools")
[[ $have == "$want" ]] && pass "tools/list offers exactly the eight read tools" || fail "tools/list offers exactly the eight read tools" "have: $have"
raw=$(jq -r '[.[] | select(.inputSchema.properties // {} | keys | any(test("^(query|document|graphql|operation)$"; "i"))) | .name] | join(" ")' <<<"$tools")
[[ -z $raw ]] && pass "no tool takes GraphQL from the agent" || fail "no tool takes GraphQL from the agent" "these do: $raw"
short=$(jq -r '[.[] | select((.description // "") | split(" ") | length < 12) | .name] | join(" ")' <<<"$tools")
[[ -z $short ]] && pass "every tool has a description of 12 words or more" || fail "every tool has a description of 12 words or more" "too short: $short"
req=$(jq -c '[.[] | {(.name): (.inputSchema.required // [] | sort)}] | add' <<<"$tools")
[[ $(jq -c '{book_details, club_details, club_meetups, find_books, list_clubs, reader_profile, reader_reviews, search}' <<<"$req") == \
   '{"book_details":["id"],"club_details":["id"],"club_meetups":["clubId"],"find_books":[],"list_clubs":[],"reader_profile":["id"],"reader_reviews":["id"],"search":["text"]}' ]] \
  && pass "required parameters: ids and the search text; everything else optional" || fail "required parameters" "$req"

# Each tool must answer exactly what its operation answers when Shelfie runs it directly.
direct() { gql_ref "$(cat $OPS/$1.graphql)" "$2"; }
gql_ref() { jq -n --arg q "$1" --argjson v "$2" '{query: $q, variables: $v}' | curl -s $REF/graphql -H 'content-type: application/json' --data-binary @- | jq -cS .data; }
same=0 total=0 diffs=()
check() { # tool args operation vars
  total=$((total + 1))
  local got want; got=$(mcp_text "$1" "$2" | jq -cS . 2>/dev/null); want=$(direct "$3" "$4")
  [[ -n $got && $got == "$want" ]] && same=$((same + 1)) || diffs+=("$1 $2")
}
check find_books '{"titleContains":"the"}' FindBooks '{"titleContains":"the"}'
check find_books '{}' FindBooks '{}'
check find_books '{"first":2}' FindBooks '{"first":2}'
check book_details '{"id":"b4"}' BookDetails '{"id":"b4"}'
check book_details '{"id":"b999"}' BookDetails '{"id":"b999"}'
check list_clubs '{}' ListClubs '{}'
check club_details '{"id":"c2"}' ClubDetails '{"id":"c2"}'
check club_meetups '{"clubId":"c1","upcomingOnly":false}' ClubMeetups '{"clubId":"c1","upcomingOnly":false}'
check reader_profile '{"id":"u1"}' ReaderProfile '{"id":"u1"}'
check reader_reviews '{"id":"u10"}' ReaderReviews '{"id":"u10"}'
check search '{"text":"asha"}' Search '{"text":"asha"}'
(( same == total )) && pass "all eight tools answer what their operations answer ($total calls)" \
  || { fail "$((total - same)) of $total tool calls differ from the operation run directly"; printf '        %s\n' "${diffs[@]}"; }
[[ $(mcp_call book_details "{}" | jq -r .isError 2>/dev/null) == true ]] && pass "a call without a required id is a tool error" || fail "a call without a required id is a tool error"
best=$(mcp_text list_clubs | jq -r 2>/dev/null '[.clubs[] | select(.visibility == "PUBLIC" and .currentBook != null)] | max_by(.currentBook.averageRating) | .name' | tr 'A-Z' 'a-z' | tr -d '\n' | sha256sum | cut -c1-16)
[[ $best == "$(jq -r '.questions[0].answers[0]' /course/modules/m18-frontier/contract/agent/questions.json)" ]] \
  && pass "list_clubs answers the lab's question (which public club reads the highest-rated book)" || fail "list_clubs answers the lab's question"
notes_check CONNECT.md 80 m18/02
finish

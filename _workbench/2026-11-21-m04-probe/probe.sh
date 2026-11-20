#!/usr/bin/env bash
# Run inside the course image: bin/course shell -c 'bash _workbench/2026-11-21-m04-probe/probe.sh'
source /course/tools/testlib.sh
ref_reset_data
echo "## typed: anonymous, bad input, ok"
Q='mutation($i: CreateReviewInput!) { createReviewTyped(input: $i) { __typename ... on ReviewCreated { review { id } } ... on ValidationFailed { problems { field message } } ... on NotAllowed { reason } } }'
gql_as "" "$Q" '{"i":{"bookId":"b2","rating":5,"body":"long enough body"}}'; echo
gql_as u4 "$Q" '{"i":{"bookId":"zz","rating":9,"body":"short"}}'; echo
gql_as u4 "$Q" '{"i":{"bookId":"b2","rating":5,"body":"long enough body"}}'; echo
echo "## join/leave"
J='mutation($c: ID!) { joinClub(input: {clubId: $c}) { club { id memberCount } userErrors { code message } } }'
gql_as u5 "$J" '{"c":"c1"}'; echo; gql_as u5 "$J" '{"c":"c1"}'; echo; gql_as u1 "$J" '{"c":"c2"}'; echo; gql_as u2 "$J" '{"c":"c3"}'; echo
L='mutation($c: ID!) { leaveClub(input: {clubId: $c}) { club { id memberCount } userErrors { code message } } }'
gql_as u11 "$L" '{"c":"c1"}'; echo; gql_as u5 "$L" '{"c":"c1"}'; echo
echo "## delete"
D='mutation($id: ID!) { deleteReview(input: {id: $id}) { deletedReviewId book { id averageRating } userErrors { code message } } }'
gql_as u2 "$D" '{"id":"r1"}'; echo; gql_as u1 "$D" '{"id":"r1"}'; echo
echo "## upload as JSON variable"
U='mutation($b: ID!, $f: Upload!) { uploadCover(bookId: $b, file: $f) { bytes sha256 serverBufferedBytes userErrors { code } } }'
gql_as u1 "$U" '{"b":"b1","f":"aGVsbG8="}'; echo
echo "## multipart without preflight header"
head -c 1048576 /dev/urandom > /tmp/cover.jpg
OPS=$(jq -nc --arg q "$U" '{query: $q, variables: {b: "b1", f: null}}')
curl -sS -o - -w ' HTTP %{http_code}\n' "$REF/graphql" -F "operations=$OPS" -F 'map={"0":["variables.f"]}' -F "0=@/tmp/cover.jpg;type=image/jpeg"
echo "## multipart with header"
curl -sS "$REF/graphql" -H 'GraphQL-Require-Preflight: 1' -F "operations=$OPS" -F 'map={"0":["variables.f"]}' -F "0=@/tmp/cover.jpg;type=image/jpeg"; echo
sha256sum /tmp/cover.jpg
echo "## presigned"
C='mutation($b: ID!) { createCoverUpload(input: {bookId: $b, contentType: "image/jpeg"}) { uploadUrl uploadId userErrors { code } } }'
R=$(gql_as u1 "$C" '{"b":"b2"}'); echo "$R" | jq -c '.data.createCoverUpload | {uploadId, url: (.uploadUrl|.[0:60])}'
URL=$(jq -r .data.createCoverUpload.uploadUrl <<<"$R"); ID=$(jq -r .data.createCoverUpload.uploadId <<<"$R")
F='mutation($u: ID!) { confirmCoverUpload(input: {uploadId: $u}) { book { id coverUrl } bytes sha256 serverBufferedBytes userErrors { code message } } }'
gql_as u1 "$F" "{\"u\":\"$ID\"}"; echo
curl -sS -o /dev/null -w 'PUT %{http_code}\n' -X PUT -H 'Content-Type: image/jpeg' --data-binary @/tmp/cover.jpg "$URL"
gql_as u1 "$F" "{\"u\":\"$ID\"}"; echo
curl -sS -o /dev/null -w 'PUT wrong type %{http_code}\n' -X PUT -H 'Content-Type: image/png' --data-binary @/tmp/cover.jpg "$URL"
ref_reset_data
gql_as "" '{ book(id: "b1") { averageRating coverUrl } club(id: "c1") { memberCount } }'; echo

#!/usr/bin/env bash
set -euo pipefail
REF="${REF:-http://shelfie-ref:8080}"
GRPC="${GRPC:-shelfie-ref:9090}"
CLUB="${1:-c1}"

echo "REST: $(curl -fsS "$REF/api/clubs/$CLUB" | jq -r .name)"
echo "GRPC: $(grpcurl -plaintext -d "{\"id\":\"$CLUB\"}" "$GRPC" shelfie.v1.ShelfieService/GetClub | jq -r .name)"
echo "GRAPHQL: $(jq -n --arg id "$CLUB" '{query: "query($id: ID!) { club(id: $id) { name } }", variables: {id: $id}}' \
  | curl -fsS -H 'Content-Type: application/json' --data-binary @- "$REF/graphql" | jq -r .data.club.name)"
# grpcurl prints the .proto field names (member_count), not the lowerCamelCase JSON names
echo "GRPC_PAGE_MEMBERS: $(grpcurl -plaintext -d "{\"club_id\":\"$CLUB\"}" "$GRPC" shelfie.v1.ShelfieService/GetClubPage | jq -r .member_count)"

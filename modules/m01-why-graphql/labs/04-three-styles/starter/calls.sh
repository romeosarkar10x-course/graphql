#!/usr/bin/env bash
# Lab 1.4: ask the reference server for the same thing three ways.
# Runs inside the course image: bin/course shell, then  bash calls.sh
# Each command must print exactly the club name, e.g.  The Midnight Readers
set -euo pipefail
REF="${REF:-http://shelfie-ref:8080}"
GRPC="${GRPC:-shelfie-ref:9090}"
CLUB="${1:-c1}"

echo "REST: $(echo TODO)"       # curl the REST resource, pick .name with jq
echo "GRPC: $(echo TODO)"       # grpcurl -plaintext ... shelfie.v1.ShelfieService/GetClub
echo "GRAPHQL: $(echo TODO)"    # curl a POST to /graphql with a variable for the id
echo "GRPC_PAGE_MEMBERS: $(echo TODO)"  # GetClubPage: print its member count
